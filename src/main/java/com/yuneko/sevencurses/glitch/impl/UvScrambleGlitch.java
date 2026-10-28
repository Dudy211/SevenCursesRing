package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 经纬错织：纹理坐标被随机拉偏，观感是条纹 / 糊成色块 —— 最像「显存坏了」的一种。
 *
 * <p>实际改写发生在 {@link com.yuneko.sevencurses.mixin.TextureAtlasSpriteMixin}。
 */
public final class UvScrambleGlitch implements RenderGlitch {

    public static final String ID = "uv_scramble";

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
        float amount = CurseConfig.SPEC.isLoaded()
                ? CurseConfig.UV_AMOUNT.get().floatValue()
                : 0.25f;
        GlitchRuntime.seed = random.nextInt();
        GlitchRuntime.uvAmount = amount;
        GlitchRuntime.uvScramble = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.uvScramble = false;
    }
}
