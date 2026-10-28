package com.yuneko.sevencurses.glitch.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 深浅失序：随机几秒关掉深度测试，物体互相穿插、忽隐忽现。
 *
 * <p>实际改写发生在 {@link com.yuneko.sevencurses.mixin.LevelRendererMixin}：
 * HEAD 里 {@code disableDepthTest()}，RETURN 里 {@code enableDepthTest()}。
 *
 * <p>{@link #stop()} 里会再无条件还原一次 —— 即使某帧 RETURN 注入没跑到
 * （比如渲染中抛异常），也不会把深度测试留在关闭状态。
 */
public final class DepthChaosGlitch implements RenderGlitch {

    public static final String ID = "depth_chaos";

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
        GlitchRuntime.seed = random.nextInt();
        GlitchRuntime.depthChaos = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.depthChaos = false;
        // 保险：无论 mixin 那对注入有没有跑全，这里强制还原
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        } catch (Throwable ignored) {
            // 渲染线程以外调用会抛 —— 不致命，下一帧 vanilla 自己会重设
        }
    }
}
