package com.yuneko.sevencurses.glitch;

/**
 * 故障强度分级。配置里的 {@code maxIntensity} 会按这个等级过滤候选故障。
 *
 * <p>LIGHT  = 只动贴图 / 画点假花屏（纯粹视觉，最安全）
 * <p>MEDIUM = 动 GL 状态（深度 / 混合 / 色调），必须 try-finally 还原
 * <p>HEAVY  = 动矩阵 / 冻结帧 / 假死演出
 */
public enum GlitchSeverity {
    LIGHT(0),
    MEDIUM(1),
    HEAVY(2);

    private final int level;

    GlitchSeverity(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }
}
