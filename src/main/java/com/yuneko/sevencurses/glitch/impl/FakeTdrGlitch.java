package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 假死演出（纯表演，零风险）。
 *
 * <p>流程：
 * <ol>
 *   <li>画面被黑幕盖住（复用 signal_loss 的黑幕绘制）；</li>
 *   <li>0.7 秒后中间浮出一个仿 Windows 的
 *       「显示驱动程序已停止响应并且已恢复」对话框；</li>
 *   <li>对话框上方还浮一行假的 {@code nvlddmkm} 驱动名 + TDR 计数；</li>
 *   <li>整个故障结束后一切消失（不弹真 Screen、不暂停游戏、不阻塞输入）。</li>
 * </ol>
 *
 * <p>全部只是 HUD 上画矩形和文字，不碰 GL 状态、不碰帧缓冲。
 */
public final class FakeTdrGlitch implements RenderGlitch {

    public static final String ID = "fake_tdr";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.HEAVY;
    }

    @Override
    public void start(RandomSource random) {
        GlitchRuntime.seed = random.nextInt();
        GlitchRuntime.fakeTdrStartMs = System.currentTimeMillis();
        GlitchRuntime.signalLossAlpha = 0xFF;
        GlitchRuntime.signalLoss = true;
        GlitchRuntime.fakeTdr = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.fakeTdr = false;
        GlitchRuntime.signalLoss = false;
        GlitchRuntime.fakeTdrStartMs = 0L;
    }
}
