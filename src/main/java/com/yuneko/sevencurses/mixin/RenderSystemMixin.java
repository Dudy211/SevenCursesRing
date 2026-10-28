package com.yuneko.sevencurses.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「垂烬」· 两个拦截点，都在 {@link RenderSystem} 上：
 *
 * <ol>
 *   <li>{@code setShaderColor} —— 颜色叛变：把每次设色乘上滤镜。
 *       好处：glitch 结束自动恢复（每帧现算，不存在"忘了还原"）。</li>
 *   <li>{@code setShaderTexture(int, ResourceLocation)} —— 贴图遗失：
 *       凡是绑定 {@code textures/atlas/*} 的，换成我们注册的紫黑棋盘贴图。
 *       这是让<b>方块</b>真变紫黑的唯一可行路线（方块 UV 是烘焙死的，
 *       改 getSprite / getU0 在渲染时根本不会被读到）。</li>
 * </ol>
 *
 * <p>两个注入都有重入保护，避免自己调自己无限递归。
 */
@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    /* ---------- 颜色叛变 ---------- */

    @Inject(method = "setShaderColor(FFFF)V", at = @At("HEAD"), cancellable = true)
    private static void sevencurses$tint(float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!GlitchRuntime.colorWarp || GlitchRuntime.tintGuard) {
            return;
        }
        GlitchRuntime.tintGuard = true;
        try {
            RenderSystem.setShaderColor(
                    red * GlitchRuntime.warpR,
                    green * GlitchRuntime.warpG,
                    blue * GlitchRuntime.warpB,
                    alpha);
        } finally {
            GlitchRuntime.tintGuard = false;
        }
        ci.cancel();
    }

    /* ---------- 贴图遗失（换绑 atlas）---------- */

    @Inject(
            method = "setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sevencurses$swapAtlasTexture(int slot, ResourceLocation location, CallbackInfo ci) {
        if (!GlitchRuntime.missingTexture || GlitchRuntime.texGuard) {
            return;
        }
        if (!GlitchRuntime.isAtlas(location)) {
            return;
        }
        // 把方块/粒子图集换成我们的紫黑棋盘。
        // MISSING_TEX 的 path 是 "missingno"，不含 "textures/atlas/"，所以不会自匹配 → 无递归。
        GlitchRuntime.texGuard = true;
        try {
            RenderSystem.setShaderTexture(slot, GlitchRuntime.MISSING_TEX);
        } finally {
            GlitchRuntime.texGuard = false;
        }
        ci.cancel();
    }
}
