package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 失手：随机「完全失灵」或「反转」。
 *
 * <p>实际改写在 {@link com.yuneko.sevencurses.mixin.MouseHandlerMixin}：
 * <ul>
 *   <li>{@code mouseDead}  → 取消 {@code turnPlayer()} + 清零 accumulatedDX/DY → 鼠标完全不动</li>
 *   <li>{@code mouseInvert} → 翻转 accumulatedDX/DY 符号 → 鼠标反转（往上往下、往左往右反着来）</li>
 * </ul>
 *
 * <p>用 {@code @Accessor}（不是 @Shadow）访问私有字段 —— 因为 @Accessor 会被 AP 写进 refmap，
 * 生产环境能映射到 SRG 字段名；@Shadow 字段不会。
 */
public final class MouseFailureGlitch implements RenderGlitch {

    public static final String ID = "mouse_failure";

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
        return 2;
    }

    @Override
    public void start(RandomSource random) {
        GlitchRuntime.seed = random.nextInt();
        if (random.nextBoolean()) {
            GlitchRuntime.mouseDead = true;
            GlitchRuntime.mouseInvert = false;
        } else {
            GlitchRuntime.mouseDead = false;
            GlitchRuntime.mouseInvert = true;
        }
    }

    @Override
    public void stop() {
        GlitchRuntime.mouseDead = false;
        GlitchRuntime.mouseInvert = false;
    }
}
