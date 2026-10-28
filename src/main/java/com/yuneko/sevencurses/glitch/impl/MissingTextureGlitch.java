package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 贴图遗失：方块 / 物品全部回退成经典的「紫黑缺失贴图」。
 *
 * <p>⚠️ v3 大改 —— 之前走 {@code TextureAtlas.getSprite} 是错的：
 * 方块 UV 在<b>烘焙时</b>就烧进了四边形顶点数据，渲染时根本不读 getSprite。
 *
 * <p>正确做法：在 {@link com.yuneko.sevencurses.mixin.RenderSystemMixin} 里拦截
 * {@code RenderSystem.setShaderTexture(int, ResourceLocation)}，凡是绑定
 * {@code textures/atlas/*} 的（方块图集、粒子图集……）一律换成我们注册的
 * {@link GlitchRuntime#MISSING_TEX}（16x16 紫黑棋盘）。
 *
 * <p>效果：方块图集被整张换掉 → 所有方块 / 物品的面都采样到紫黑棋盘 →
 * 经典的「缺失贴图」观感。换回来一行 flag 即可，无残留。
 */
public final class MissingTextureGlitch implements RenderGlitch {

    public static final String ID = "missing_texture";

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
        GlitchRuntime.seed = random.nextInt();
        // 先建贴图（幂等），再打开 flag —— 顺序很重要：
        // flag 打开之后 mixin 才会开始换绑，所以贴图必须先就位
        GlitchRuntime.ensureMissingTexture();
        GlitchRuntime.missingTexture = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.missingTexture = false;
    }
}
