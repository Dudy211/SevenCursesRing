package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 显存碎屑：屏幕随机出现显存坏点式的彩条 / 撕裂行。
 *
 * <p>零风险的一种 —— 只是往 HUD 上画东西，不碰任何 GL 状态。
 * 实际绘制在 {@link com.yuneko.sevencurses.client.CurseHudOverlay}。
 */
public final class ArtifactBlocksGlitch implements RenderGlitch {

    public static final String ID = "artifact_blocks";

    /** 上限，别画太多 —— 每条都是真的一次 draw call。 */
    private static final int MAX_BLOCKS = 24;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.LIGHT;
    }

    @Override
    public void start(RandomSource random) {
        GlitchRuntime.artifactSeed = random.nextLong();
        GlitchRuntime.artifactCount = 6 + random.nextInt(MAX_BLOCKS - 6);
        GlitchRuntime.artifacts = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.artifacts = false;
        GlitchRuntime.artifactCount = 0;
    }
}
