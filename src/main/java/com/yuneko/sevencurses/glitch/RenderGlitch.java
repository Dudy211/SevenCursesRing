package com.yuneko.sevencurses.glitch;

import net.minecraft.util.RandomSource;

/**
 * 一个可随机播放的渲染故障。
 *
 * <p>约定：{@link #start} 只负责写 {@link GlitchRuntime} 的状态位，
 * 真正的渲染改写在对应的 Mixin / HUD 里读状态位执行。
 * {@link #stop()} 必须把状态位复原 —— 调度器保证一定会调用它。
 */
public interface RenderGlitch {

    /** 稳定 id，用于配置、指令和冷却表。 */
    String id();

    GlitchSeverity severity();

    void start(RandomSource random);

    void stop();

    /** 被抽中的权重（默认 1）。越大的越常被选中。 */
    default int weight() {
        return 1;
    }

    /** 持续时间倍率（默认 1.0）。用来让某些故障比全局默认更长。 */
    default float durationScale() {
        return 1.0f;
    }

    /**
     * 播放期间的每 tick 回调（客户端 tick）。
     *
     * <p>默认什么都不做。给那些"需要在播放期间持续核对自身是否真的生效"的故障用 ——
     * 目前只有 {@code tps_spike}（Mixin 死了要自己发现并切兜底通道）。
     */
    default void onTick() {
    }
}
