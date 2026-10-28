package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.ServerGlitchState;
import com.yuneko.sevencurses.SevenCursesMod;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 迟世（TPS 抽风）：在<b>服务端 tick</b>上随机睡一下 → mspt 飙升 → TPS 真掉。
 *
 * <p>跟 {@code fps_spike}（客户端帧率）不一样：这个是<b>世界</b>卡 ——
 * 怪物不动、方块更新延迟、掉落物悬停，但 FPS 数字是正常的。
 *
 * <p><b>v1.4.0 自愈：</b>如果开起来 10 个 tick 之后
 * {@link ServerGlitchState#tickSeen} 一次都没涨，说明 {@code MinecraftServer.tickServer}
 * 的 Mixin 没挂上（换版本 / 别的模组抢了 / 映射漂了）。
 * 这时自动切到 {@link ServerGlitchState#eventFallback}，由 {@code ServerTickEvent}
 * 在服务端线程补睡 —— 效果一样，只是路径不同。日志里会留一行 WARN。
 */
public final class TpsSpikeGlitch implements RenderGlitch {

    public static final String ID = "tps_spike";

    /** 开咒那一刻的 tickSeen 快照，用来判断 Mixin 有没有在跑。 */
    private static long seenAtStart;
    /** 已经观察了几个 tick。 */
    private static int watchTicks;
    /** 本次是否已经因为 Mixin 没跑而打过 WARN（避免刷屏）。 */
    private static boolean warned;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.HEAVY;
    }

    @Override
    public int weight() {
        return 3;   // 抽中概率是普通故障的 3 倍
    }

    @Override
    public float durationScale() {
        return 2.0f;   // 持续 2 倍长
    }

    @Override
    public void start(RandomSource random) {
        ServerGlitchState.resetRun();
        ServerGlitchState.tpsSpike = true;
        seenAtStart = ServerGlitchState.tickSeen;
        watchTicks = 0;
        warned = false;
    }

    /**
     * 由调度器每 tick 调用 —— 只做一件事：确认 Mixin 真的在跑。
     *
     * <p>10 个 tick（0.5 秒）足够集成服务器跑十几次 tickServer 了。
     * 一次都没涨 = Mixin 死了 → 打开兜底通道。
     */
    @Override
    public void onTick() {
        if (!ServerGlitchState.tpsSpike) {
            return;
        }
        long seen = ServerGlitchState.tickSeen;
        if (seen != seenAtStart) {
            // Mixin 活着：兜底通道就没必要开着（避免两边都睡，掉到 1 TPS）
            if (ServerGlitchState.eventFallback) {
                ServerGlitchState.eventFallback = false;
            }
            return;
        }
        if (++watchTicks < 10) {
            return;
        }
        if (!ServerGlitchState.eventFallback) {
            ServerGlitchState.eventFallback = true;
            if (!warned) {
                warned = true;
                SevenCursesMod.LOG.warn(
                        "[SevenCurses] tickServer 的 Mixin 十个 tick 一次都没跑 —— "
                                + "已切到 ServerTickEvent 兜底，TPS 照掉不误。");
            }
        }
    }

    @Override
    public void stop() {
        ServerGlitchState.tpsSpike = false;
        ServerGlitchState.eventFallback = false;
    }
}
