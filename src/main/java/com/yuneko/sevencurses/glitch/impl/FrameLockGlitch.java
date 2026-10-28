package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;

/**
 * 停钟：把 framerateLimit 打到 5~15 FPS。
 *
 * <p>跟 {@code fps_spike}（乱跳）不同：这个是<b>持续锁死</b>在很低帧率，
 * 画面像幻灯片一样一格一格动。停了立刻恢复原值。
 */
public final class FrameLockGlitch implements RenderGlitch {

    public static final String ID = "frame_lock";

    private static boolean saved = false;
    private static int origLimit;

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
        saved = true;
    }

    @Override
    public void stop() {
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
