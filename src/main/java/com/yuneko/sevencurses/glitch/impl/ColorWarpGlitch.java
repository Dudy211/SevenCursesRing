package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 颜色叛变：整个世界被染成紫 / 绿 / 负片。
 *
 * <p>实现方式不是「设一次全局颜色」—— 那样一帧内就被 MC 自己的绘制覆盖了。
 * 而是挂在 {@link com.yuneko.sevencurses.mixin.RenderSystemMixin} 里，
 * 拦截<b>每一次</b> {@code RenderSystem.setShaderColor}，把传入值乘上滤镜。
 * 好处：glitch 结束的一瞬间自动恢复（因为每帧都重新算），不需要还原动作。
 */
public final class ColorWarpGlitch implements RenderGlitch {

    public static final String ID = "color_warp";

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
        float r = 0.25f + random.nextFloat() * 1.5f;
        float g = 0.25f + random.nextFloat() * 1.5f;
        float b = 0.25f + random.nextFloat() * 1.5f;

        // 三种风格随机：偏色 / 压暗某一通道 / 近似负片
        int style = random.nextInt(3);
        if (style == 1) {
            int kill = random.nextInt(3);
            if (kill == 0) {
                r *= 0.2f;
            } else if (kill == 1) {
                g *= 0.2f;
            } else {
                b *= 0.2f;
            }
        }

        GlitchRuntime.seed = random.nextInt();
        GlitchRuntime.warpR = r;
        GlitchRuntime.warpG = g;
        GlitchRuntime.warpB = b;
        GlitchRuntime.colorWarp = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.colorWarp = false;
        GlitchRuntime.warpR = 1.0f;
        GlitchRuntime.warpG = 1.0f;
        GlitchRuntime.warpB = 1.0f;
    }
}
