package com.yuneko.sevencurses.client;

import com.yuneko.sevencurses.SevenCursesMod;
import com.yuneko.sevencurses.boon.HardwareProfile;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/**
 * 客户端硬件探针 —— 只干一件事：把「机器有多好」写进 {@link HardwareProfile}。
 *
 * <p>为什么不在 mod 构造期读：那时还没有 GL 上下文，{@code glGetString} 会拿到 null。
 * 所以拖到第一个客户端 tick（主线程、上下文已就绪）。
 *
 * <p>★ 显卡只能<b>粗略分档</b>：拿 {@code GL_RENDERER} 字符串（例如
 * {@code "NVIDIA GeForce RTX 4060/PCIe/SSE2"}），按关键字 + 型号数字分 0~3 档。
 * 没有可靠的性能查询接口，这是能做到的最好程度，README 里也这么写了。
 */
public final class HardwareProbe {

    private static boolean done = false;

    private HardwareProbe() {
    }

    /** 由 ClientBootstrap 每个客户端 tick 调用；探到一次就不再探。 */
    public static void tick() {
        if (done) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.options == null) {
            return;
        }
        done = true;

        HardwareProfile.probeJvm();
        HardwareProfile.viewDistance = mc.options.renderDistance().get();

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w > 0 && h > 0) {
            // 下限 1280x720：把窗口缩成一小条不该能白拿经验加成
            HardwareProfile.screenPixels = Math.max(w * h, 1280 * 720);
        }

        String renderer = "";
        try {
            renderer = GL11.glGetString(GL11.GL_RENDERER);
        } catch (Throwable ignored) {
            // 拿不到就当中等档
        }
        HardwareProfile.gpuTier = tierOf(renderer);
        HardwareProfile.probed = true;

        SevenCursesMod.LOG.info("[SevenCurses] 硬件探测：{} → gpuTier={}（{}）",
                renderer == null ? "?" : renderer, HardwareProfile.gpuTier,
                HardwareProfile.describe());
    }

    /* ---------------- 帧率采样 ---------------- */

    private static int frames = 0;
    private static long windowStartMs = 0L;
    private static int samples = 0;
    /** 第一帧的时间戳；用来做「采样卡住」的兜底。 */
    private static long firstFrameMs = 0L;

    /**
     * 由渲染事件每帧调一次（{@code RenderLevelStageEvent}）。
     *
     * <p>只采 <b>2 个 1 秒窗口</b>（取较小值，避开刚进世界那阵卡顿），然后冻结。
     * 冻结之后不再更新 —— 否则玩家抬头看天就能刷出高分，七福就成自选菜单了。
     *
     * <p>★ 采样期间「缓」不会压帧率（{@code fpsMeasured} 为 false 时让路），
     * 所以量到的是这台机器的真实表现，而不是被七咒掐过之后的 10fps。
     */
    public static void onFrame() {
        long now = System.currentTimeMillis();
        if (firstFrameMs == 0L) {
            firstFrameMs = now;
        }

        // ★ 闸 2 兜底：15 秒还没采到能力值（极端卡顿 / 一直进不去世界），用现有值收工。
        //   宁可采样不准，也不能让故障调度器永远饿着 —— 那才是真的"没生效"。
        if (!HardwareProfile.fpsMeasured && now - firstFrameMs > 15_000L) {
            HardwareProfile.fpsMeasured = true;
            SevenCursesMod.LOG.warn("[SevenCurses] 帧率采样超时（15s），按 {} fps 收工。",
                    String.format(java.util.Locale.ROOT, "%.0f", HardwareProfile.peakFps));
        }

        if (windowStartMs == 0L) {
            windowStartMs = now;
            frames = 0;
            return;
        }
        frames++;
        long elapsed = now - windowStartMs;
        if (elapsed < 1000L) {
            return;
        }

        double fps = frames * 1000.0 / elapsed;
        frames = 0;
        windowStartMs = now;

        // ① 实时值：一直更新（EMA 平滑，别让一瞬间的抖动改护甲）
        double ema = HardwareProfile.fpsEma;
        HardwareProfile.fpsEma = ema <= 0.0 ? fps : ema * 0.6 + fps * 0.4;

        // ② 顺手刷新「会变」的输入：视距 / 窗口像素（明、远两条福要实时）
        refreshDynamicInputs();

        // ③ 能力值：前两个窗口取较小值，然后冻结（防抬头看天刷分）
        if (!HardwareProfile.fpsMeasured) {
            if (samples == 0 || fps < HardwareProfile.peakFps) {
                HardwareProfile.peakFps = fps;
            }
            samples++;
            if (samples >= 2) {
                HardwareProfile.fpsMeasured = true;
                SevenCursesMod.LOG.info("[SevenCurses] 能力值采样完成：{} fps（{}）",
                        String.format(java.util.Locale.ROOT, "%.0f", HardwareProfile.peakFps),
                        HardwareProfile.describe());
            }
        }
    }

    /**
     * 每秒刷新一次「会变」的输入。
     *
     * <ul>
     *   <li>{@code viewDistance} —— 「远」：中途改视距立刻生效。</li>
     *   <li>{@code screenPixels} —— 「明」：按<b>窗口</b>像素算，但<b>下限 1280×720</b>，
     *       否则把窗口缩成一小条就能白拿经验加成。</li>
     * </ul>
     */
    private static void refreshDynamicInputs() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        HardwareProfile.viewDistance = mc.options.renderDistance().get();
        if (mc.getWindow() != null) {
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            if (w > 0 && h > 0) {
                HardwareProfile.screenPixels = Math.max(w * h, 1280 * 720);
            }
        }
    }

    /**
     * 显卡分档：0 = 核显/软件渲染，1 = 入门独显，2 = 中端，3 = 高端。
     *
     * <p>纯启发式。判错的代价只是七福强度偏一档，不影响稳定性。
     */
    static int tierOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        String s = raw.toLowerCase(java.util.Locale.ROOT);

        // 软件渲染：最弱 → 给最强补偿
        if (s.contains("llvmpipe") || s.contains("softpipe") || s.contains("swiftshader")
                || s.contains("software rasterizer")) {
            return 0;
        }
        // 核显
        if (s.contains("uhd graphics") || s.contains("hd graphics")) {
            return 0;
        }
        if (s.contains("intel") && !s.contains("arc")) {
            return 0;
        }
        if (s.contains("adreno") || s.contains("mali") || s.contains("powervr")) {
            return 0;
        }
        // 苹果统一内存：算中端
        if (s.contains("apple m")) {
            return 2;
        }

        int num = biggestModelNumber(s);
        if (num <= 0) {
            return 1;   // 认不出来 → 当作入门独显
        }
        if (num >= 4080 || num >= 3080 || num >= 6800 || num >= 7800 || num >= 2080) {
            return 3;
        }
        if (num >= 4060 || num >= 3060 || num >= 6600 || num >= 7600 || num >= 2070
                || num >= 5700 || num >= 1660) {
            return 2;
        }
        return 1;
    }

    /** 从字符串里捞一个最像型号的 4 位数字（RTX 4060 → 4060，RX 6800 → 6800）。 */
    private static int biggestModelNumber(String s) {
        int best = -1;
        int i = 0;
        while (i < s.length()) {
            if (!Character.isDigit(s.charAt(i))) {
                i++;
                continue;
            }
            int j = i;
            while (j < s.length() && Character.isDigit(s.charAt(j))) {
                j++;
            }
            if (j - i == 4) {
                int v = Integer.parseInt(s.substring(i, j));
                if (v > best) {
                    best = v;
                }
            }
            i = j;
        }
        return best;
    }
}
