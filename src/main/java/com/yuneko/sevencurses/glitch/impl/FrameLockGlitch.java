package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;

/**
 * 第 1 咒 · 锁帧率：把 framerateLimit 打到 5~15 FPS。
 *
 * <p>跟 {@code fps_spike}（乱跳）不同：这个是<b>持续锁死</b>在很低帧率，
 * 画面像幻灯片一样一格一格动。停了立刻恢复原值。
 */
public final class FrameLockGlitch implements RenderGlitch {

    public static final String ID = "frame_lock";

    private static boolean saved = false;
    private static int origLimit;

    /**
     * 是否正在播放「停钟」。
     *
     * <p>「缓」（七福）也要写 framerateLimit，两者必须互斥 ——
     * 停钟期间让七福让路，否则 5~15fps 的暴击会被立刻覆盖成常驻上限，咒就白放了。
     */
    public static volatile boolean active = false;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.MEDIUM;
    }

    @Override
    public void start(RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        origLimit = mc.options.framerateLimit().get();
        mc.options.framerateLimit().set(5 + random.nextInt(11));  // 5~15 FPS
        active = true;
        saved = true;
    }

    @Override
    public void stop() {
        active = false;
        if (!saved) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.framerateLimit().set(origLimit);
        }
        saved = false;
    }
}
