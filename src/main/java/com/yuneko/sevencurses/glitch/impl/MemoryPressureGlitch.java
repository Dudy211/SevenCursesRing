package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 淤积（锁内存）。
 *
 * <p><b>v1.3.0 为什么"没起作用"：</b>旧版只做了「吃掉 20% 堆」这一件事。
 * 20% 的堆对 G1 来说只是一次普通的堆增长 —— 它不产生持续压力，
 * 只会让 F3 的 Mem 数字静静高一点，然后 2~6 秒后放掉，人几乎察觉不到。
 * 而且如果开局堆已经很满，看门狗会当场判定「剩余 < 15%」，
 * 直接一个字节都不分配 —— 于是彻底无事发生。
 *
 * <p><b>v1.4.0 改成两层，都要有：</b>
 * <ol>
 *   <li><b>占位（held）</b>：吃 {@code maxHeap × holdFraction}（默认 25%），
 *       下限 256MB、上限 3GB，且**硬保证分配后剩余 ≥ 25% 堆**。
 *       这部分是稳定占着的，F3 的 Mem 会明明白白高出一大块。</li>
 *   <li><b>搅动（churn）</b>：一条守护线程不停 new 4MB 数组、只保留最近 12 个
 *       （≈48MB 活动集），其余立刻变垃圾 → young GC 每几百毫秒一次 →
 *       <b>真的会一顿一顿的</b>。这才是"锁内存"该有的手感。</li>
 * </ol>
 *
 * <p>⚠️ 安全边界（一条都不能破）：
 * <ul>
 *   <li>占用硬上限 = holdFraction（≤ 0.45）× maxHeap，且分配后必须留 25% 空</li>
 *   <li>搅动线程每轮都检查剩余堆，低于 12% 就歇 25ms，绝不在低空盘旋</li>
 *   <li>catch {@link OutOfMemoryError}：真撞上就立刻收手（不抛给游戏）</li>
 *   <li>stop() / panic / 关咒 → {@link #hardStop()}，池子清空 + 线程中断 + 提示 GC</li>
 *   <li>不碰存档、不分配超大单块（永远 1MB/4MB）</li>
 * </ul>
 */
public final class MemoryPressureGlitch implements RenderGlitch {

    public static final String ID = "memory_pressure";

    private static final int HOLD_CHUNK = 1024 * 1024;        // 占位 1MB 一块
    private static final int CHURN_CHUNK = 4 * 1024 * 1024;   // 搅动 4MB 一块
    private static final int CHURN_KEEP = 12;                 // 活动集 48MB
    private static final long HOLD_MIN = 256L * 1024 * 1024;  // 至少占 256MB
    private static final long HOLD_MAX = 3072L * 1024 * 1024; // 最多占 3GB
    private static final double HOLD_RESERVE = 0.25D;         // 分配后至少留 25%
    private static final double CHURN_RESERVE = 0.12D;        // 搅动时的低空红线

    private static final List<byte[]> pool = new ArrayList<>();

    private static volatile boolean churn = false;
    private static Thread churnThread;

    /** 当前实际占住多少字节（给 /curse diag 和 F3 看）。 */
    public static volatile long heldBytes = 0L;
    /** 本次的目标占位（诊断用；被看门狗截断时 held < target）。 */
    public static volatile long targetBytes = 0L;
    /** 搅动线程已经甩出多少 MB 垃圾。 */
    public static volatile long churnMb = 0L;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.MEDIUM;
    }

    @Override
    public float durationScale() {
        return 2.0f;   // 2~12 秒，够你把 F3 打开看一眼
    }

    @Override
    public void start(RandomSource random) {
        hardStop();

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();

        double fraction = 0.25D;
        boolean wantChurn = true;
        try {
            if (CurseConfig.SPEC.isLoaded()) {
                fraction = CurseConfig.MEM_HOLD_FRACTION.get();
                wantChurn = CurseConfig.MEM_CHURN.get();
            }
        } catch (Throwable ignored) {
            // 用默认值
        }

        long target = clamp((long) (max * fraction), HOLD_MIN, HOLD_MAX);
        // 再加一道硬闸：无论配置写多大，分配完必须还剩 25%
        long reserve = (long) (max * HOLD_RESERVE);
        long room = rt.freeMemory() - reserve;
        if (room < target) {
            target = Math.max(0L, room);
        }
        targetBytes = target;

        long allocated = 0L;
        try {
            while (allocated + HOLD_CHUNK <= target) {
                if (rt.freeMemory() - HOLD_CHUNK < reserve) {
                    break;
                }
                pool.add(new byte[HOLD_CHUNK]);
                allocated += HOLD_CHUNK;
            }
        } catch (OutOfMemoryError e) {
            // 真撞上了就收手，绝不让它冒到游戏里
            pool.clear();
            allocated = 0L;
        }
        heldBytes = allocated;

        if (wantChurn) {
            startChurn();
        }
        GlitchRuntime.memoryPressure = true;
    }

    /** 搅动线程：制造持续垃圾 → young GC 连绵不断 → 真卡顿。 */
    private static void startChurn() {
        churn = true;
        Thread t = new Thread(() -> {
            ArrayDeque<byte[]> ring = new ArrayDeque<>();
            Runtime rt = Runtime.getRuntime();
            long reserve = (long) (rt.maxMemory() * CHURN_RESERVE);
            while (churn) {
                try {
                    if (rt.freeMemory() - CHURN_CHUNK < reserve) {
                        Thread.sleep(25L);
                        continue;
                    }
                    ring.addLast(new byte[CHURN_CHUNK]);
                    churnMb += CHURN_CHUNK / (1024 * 1024);
                    while (ring.size() > CHURN_KEEP) {
                        ring.removeFirst();   // 变垃圾
                    }
                    Thread.sleep(4L + (long) (Math.random() * 10.0));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (OutOfMemoryError e) {
                    break;
                }
            }
            ring.clear();
        }, "sevencurses-memchurn");
        t.setDaemon(true);
        t.start();
        churnThread = t;
    }

    @Override
    public void stop() {
        hardStop();
        System.gc();   // 只是提示；被 -XX:+DisableExplicitGC 关掉也无害
    }

    /** panic / 关咒 / 换故障时的硬清理 —— 池子、线程、标志位一起收。 */
    public static void hardStop() {
        churn = false;
        Thread t = churnThread;
        churnThread = null;
        if (t != null) {
            t.interrupt();
        }
        pool.clear();
        heldBytes = 0L;
        targetBytes = 0L;
        GlitchRuntime.memoryPressure = false;
    }

    /** 诊断串：给 /curse diag 用。 */
    public static String stats() {
        return "held=" + (heldBytes / (1024 * 1024)) + "MB"
                + " target=" + (targetBytes / (1024 * 1024)) + "MB"
                + " churn=" + churnMb + "MB"
                + (churn ? " (搅动中)" : "");
    }

    private static long clamp(long v, long lo, long hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
