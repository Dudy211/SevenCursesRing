package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 呼吸不匀：在客户端 tick 里随机睡一下，制造帧率突然跳动 / 偶尔卡死一帧。
 *
 * <p>实际睡眠在 {@link com.yuneko.sevencurses.client.ClientBootstrap#onClientTick}。
 *
 * <p>⚠️ 这是「主线程 sleep」—— 不动 GL 状态、不动内存、不动存档，
 * 只是把帧往后拖。最坏情况是一次几百毫秒的卡顿，下一帧自动恢复。
 *
 * <p>顺带：F3 左上角的真实 FPS 数字会跟着掉，正好和「显卡坏了」的演出对上。
 */
public final class FpsSpikeGlitch implements RenderGlitch {

    public static final String ID = "fps_spike";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.MEDIUM;
    }

    @Override
    public int weight() {
        return 3;   // 抽中概率是普通故障的 3 倍
    }

    @Override
    public float durationScale() {
        return 2.0f;   // 持续 2 倍长
    }

    @Override
    public void start(RandomSource random) {
        GlitchRuntime.seed = random.nextInt();
        GlitchRuntime.fpsSpike = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.fpsSpike = false;
    }
}
