package com.yuneko.sevencurses.curse;

import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.RenderGlitchScheduler;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 七咒统一调度层（单例）。
 *
 * <p>默认全部关闭 —— 必须显式 {@code /curse toggle gpu}（或戴上戒指）才会开始作妖。
 * {@link #panic()} 是硬性安全出口：清空全部诅咒 + 强制复原所有渲染/按键状态。
 */
public final class CurseManager {

    private static final CurseManager INSTANCE = new CurseManager();

    private final Set<CurseType> enabled = Collections.synchronizedSet(EnumSet.noneOf(CurseType.class));
    private final RenderGlitchScheduler glitchScheduler = new RenderGlitchScheduler();
    private final KeyScrambleCurse keyScramble = new KeyScrambleCurse();

    /** 戒指是否正戴着（由 curioTick 维护）；轮询检测摘下。 */
    private volatile boolean ringEquipped = false;
    private volatile long ringLastSeenMs = 0L;

    private CurseManager() {
    }

    public static CurseManager get() {
        return INSTANCE;
    }

    public boolean globalEnabled() {
        return !CurseConfig.SPEC.isLoaded() || CurseConfig.GLOBAL_ENABLED.get();
    }

    public boolean isEnabled(CurseType type) {
        return enabled.contains(type);
    }

    public Set<CurseType> enabledCurses() {
        return Collections.unmodifiableSet(enabled);
    }

    /** @return 切换之后是否处于「开启」状态 */
    public boolean toggle(CurseType type) {
        if (enabled.remove(type)) {
            stopCurse(type);
            return false;
        }
        enabled.add(type);
        return true;
    }

    public RenderGlitchScheduler glitches() {
        return glitchScheduler;
    }

    /** 一键解除：清空全部诅咒 + 强制复原全部渲染 / 按键状态。 */
    public void panic() {
        enabled.clear();
        glitchScheduler.stopAll();
        keyScramble.stop();
        KeyScrambleRuntime.end();
        GlitchRuntime.resetAll();
    }

    /** 客户端 tick（由 ClientBootstrap 驱动）。 */
    public void clientTick() {
        // 戒指轮询：curioTick 每 tick 会刷新 ringLastSeenMs；超过 300ms 没刷新 = 摘下了
        if (ringEquipped && System.currentTimeMillis() - ringLastSeenMs > 300L) {
            ringEquipped = false;
            panic();
            return;
        }

        if (!globalEnabled()) {
            stopAllWhenDisabled();
            return;
        }
        tickGpu();
        tickKeyScramble();
    }

    /**
     * 戒指的 curioTick 回调（由 {@link com.yuneko.sevencurses.client.ClientRingHooks} 调）。
     * 戴着就强制开 GPU + 键盘错位 —— 想 /curse toggle 关也关不掉（这才是"诅咒"）。
     */
    public void ringCurioTick() {
        ringLastSeenMs = System.currentTimeMillis();
        ringEquipped = true;
        enabled.add(CurseType.GPU);
        enabled.add(CurseType.KEY_SCRAMBLE);
    }

    private void tickGpu() {
        if (isEnabled(CurseType.GPU)) {
            glitchScheduler.tick();
        } else if (glitchScheduler.isBusy()) {
            glitchScheduler.stopAll();
        }
    }

    private void tickKeyScramble() {
        if (isEnabled(CurseType.KEY_SCRAMBLE)) {
            keyScramble.tick();
        } else if (keyScramble.busy() || KeyScrambleRuntime.active) {
            keyScramble.stop();
        }
    }

    private void stopCurse(CurseType type) {
        if (type == CurseType.GPU) {
            glitchScheduler.stopAll();
        } else if (type == CurseType.KEY_SCRAMBLE) {
            keyScramble.stop();
        }
    }

    /** 总开关关掉时的收尾：把正在跑的故障全部停下并复原。 */
    private void stopAllWhenDisabled() {
        if (glitchScheduler.isBusy()) {
            glitchScheduler.stopAll();
        }
        if (keyScramble.busy() || KeyScrambleRuntime.active) {
            keyScramble.stop();
        }
    }
}
