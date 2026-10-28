package com.yuneko.sevencurses.mixin;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「垂烬」· 经纬错织。
 *
 * <p>把纹理坐标随机拉偏 —— 观感是条纹 / 糊成色块，最像「显存坏了」。
 *
 * <p>⚠️ 这四个方法在**逐顶点**路径上被调用，所以实现里：
 * <ul>
 *   <li>没有分配（{@link GlitchRuntime#jitter} 是纯算术）；</li>
 *   <li>没有加锁；</li>
 *   <li>关闭状态下第一行就 return。</li>
 * </ul>
 *
 * <p>抖动是「per-sprite 确定性」的：同一个精灵在一轮故障里抖动方式固定，
 * 换一轮 seed 才换一种拉法 —— 这样看起来像贴图坏了，而不是像在闪。
 */
@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin {

    @Inject(method = "getU0()F", at = @At("RETURN"), cancellable = true)
    private void sevencurses$scrambleU0(CallbackInfoReturnable<Float> cir) {
        if (!GlitchRuntime.uvScramble) {
            return;
        }
        cir.setReturnValue(GlitchRuntime.jitter(this, 0x01, cir.getReturnValueF()));
    }

    @Inject(method = "getU1()F", at = @At("RETURN"), cancellable = true)
    private void sevencurses$scrambleU1(CallbackInfoReturnable<Float> cir) {
        if (!GlitchRuntime.uvScramble) {
            return;
        }
        cir.setReturnValue(GlitchRuntime.jitter(this, 0x02, cir.getReturnValueF()));
    }

    @Inject(method = "getV0()F", at = @At("RETURN"), cancellable = true)
    private void sevencurses$scrambleV0(CallbackInfoReturnable<Float> cir) {
        if (!GlitchRuntime.uvScramble) {
            return;
        }
        cir.setReturnValue(GlitchRuntime.jitter(this, 0x03, cir.getReturnValueF()));
    }

    @Inject(method = "getV1()F", at = @At("RETURN"), cancellable = true)
    private void sevencurses$scrambleV1(CallbackInfoReturnable<Float> cir) {
        if (!GlitchRuntime.uvScramble) {
            return;
        }
        cir.setReturnValue(GlitchRuntime.jitter(this, 0x04, cir.getReturnValueF()));
    }
}
