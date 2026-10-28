package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 断信：画面被黑幕盖住 2 秒左右，像显示器断了信号。
 *
 * <p>实际绘制在 {@link com.yuneko.sevencurses.client.CurseHudOverlay}。
 *
 * <p>⚠️ 设计上有意做成「一次性盖住并保持」，<b>不是高频闪烁</b>：
 * 高频全屏黑白闪会有光敏性癫痫风险，那不是整活是伤害。
 */
public final class SignalLossGlitch implements RenderGlitch {

    public static final String ID = "signal_loss";

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
        GlitchRuntime.signalLossAlpha = 0xD8 + random.nextInt(0x28);
        GlitchRuntime.signalLoss = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.signalLoss = false;
    }
}
