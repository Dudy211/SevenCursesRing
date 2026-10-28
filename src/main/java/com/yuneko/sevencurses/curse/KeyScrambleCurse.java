package com.yuneko.sevencurses.curse;

import com.yuneko.sevencurses.config.CurseConfig;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 第 4 咒 · 乱指 —— 状态机。
 *
 * <p>不是「一直错位」，而是 **脉冲式**：
 * <pre>
 *   BURST(15~30 秒错位) → REST(3~8 秒正常) → BURST → ...
 * </pre>
 * 这样玩家会先以为是自己手滑，然后才慢慢发现不对劲 —— 折磨感更强，
 * 也不至于让人难受得直接退游戏。
 */
public final class KeyScrambleCurse {

    private final RandomSource random = RandomSource.create();

    private int burstLeft;
    private int restLeft = 60;

    public boolean busy() {
        return burstLeft > 0;
    }

    public void tick() {
        if (!CurseConfig.SPEC.isLoaded()) {
            return;
        }
        if (burstLeft > 0) {
            if (--burstLeft <= 0) {
                endBurst();
            }
            return;
        }
        if (restLeft > 0) {
            restLeft--;
            return;
        }
        startBurst();
    }

    public void stop() {
        if (burstLeft > 0) {
            endBurst();
        }
        restLeft = 60;
    }

    private void startBurst() {
        int min = CurseConfig.KEY_BURST_MIN.get();
        int max = Math.max(min, CurseConfig.KEY_BURST_MAX.get());
        burstLeft = Mth.nextInt(random, min, max);
        KeyScrambleRuntime.begin();
    }

    private void endBurst() {
        burstLeft = 0;
        KeyScrambleRuntime.end();
        int min = CurseConfig.KEY_REST_MIN.get();
        int max = Math.max(min, CurseConfig.KEY_REST_MAX.get());
        restLeft = Mth.nextInt(random, min, max);
    }
}
