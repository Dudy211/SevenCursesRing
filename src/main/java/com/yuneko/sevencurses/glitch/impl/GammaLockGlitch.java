package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;

/**
 * 沉夜：gamma 打到 0（最暗）+ 雾色压到接近黑。
 *
 * <p>原版 gamma 被 clamp 在 [0, 1]，到不了负数（没法真正反色），
 * 但 gamma=0 + 黑雾 = 世界几乎全黑，比单纯"暗"更恶心。
 *
 * <p>真正反色需要光影 shader（不在本 mod 能力范围内）。
 */
public final class GammaLockGlitch implements RenderGlitch {

    public static final String ID = "gamma_lock";

    private static boolean saved = false;
    private static double origGamma;

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
        origGamma = mc.options.gamma().get();
        mc.options.gamma().set(0.0);
        GlitchRuntime.gammaDark = true;
        saved = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.gammaDark = false;
        if (!saved) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.gamma().set(origGamma);
        }
        saved = false;
    }
}
