package com.yuneko.sevencurses.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「垂烬」· 深浅失序 + 空间拉伤。
 *
 * <p>目标方法签名（javap 实证）：
 * <pre>
 *   LevelRenderer.renderLevel(PoseStack, float, long, boolean, Camera, GameRenderer, LightTexture, Matrix4f)
 * </pre>
 * 调用方是 {@code GameRenderer.renderLevel(float, long, PoseStack)}，
 * 而那个 PoseStack 在 {@code GameRenderer.render} 里是 <b>偏移 233 处 new 出来的局部变量</b>。
 *
 * <p>⇒ <b>每帧一个全新的 PoseStack</b>。所以我们在这里改矩阵既不跨帧累积，
 * 也<b>完全不需要 push/pop</b> —— 因此不会踩到
 * 「push/pop 不配对 → Pose stack not empty → 一进世界就崩」那个经典坑。
 *
 * <p>Z 冲突用 HEAD/RETURN 成对开关深度测试；即使 RETURN 那半没跑到，
 * {@code DepthChaosGlitch.stop()} 里还有一次无条件还原兜底。
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    // 描述符以 GameRenderer 字节码里的调用点为准：
    //   renderLevel:(Lcom/mojang/blaze3d/vertex/PoseStack;FJZ
    //               Lnet/minecraft/client/Camera;
    //               Lnet/minecraft/client/renderer/GameRenderer;
    //               Lnet/minecraft/client/renderer/LightTexture;
    //               Lorg/joml/Matrix4f;)V
    // ⚠️ FJ 后面那个 Z（boolean outOfWorld）不能漏。
    private static final String RENDER_LEVEL =
            "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;"
                    + "Lnet/minecraft/client/renderer/GameRenderer;"
                    + "Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V";

    @Inject(method = RENDER_LEVEL, at = @At("HEAD"))
    private void sevencurses$beforeRenderLevel(PoseStack poseStack,
                                               float partialTick,
                                               long nanoTime,
                                               boolean outOfWorld,
                                               Camera camera,
                                               GameRenderer gameRenderer,
                                               LightTexture lightTexture,
                                               Matrix4f projectionMatrix,
                                               CallbackInfo ci) {
        if (GlitchRuntime.depthChaos) {
            RenderSystem.disableDepthTest();
        }

        if (GlitchRuntime.geometryWarp) {
            float amount = GlitchRuntime.geometryAmount;
            float sx = GlitchRuntime.osc(1.3f, 0.0f);
            float sy = GlitchRuntime.osc(1.9f, 1.7f);
            float sz = GlitchRuntime.osc(0.7f, 3.1f);

            poseStack.scale(1.0f + amount * sx,
                    1.0f - amount * 0.6f * sy,
                    1.0f + amount * 0.35f * sz);
            poseStack.translate(amount * 3.0f * sy, amount * 2.0f * sz, 0.0);
        }
    }

    @Inject(method = RENDER_LEVEL, at = @At("RETURN"))
    private void sevencurses$afterRenderLevel(PoseStack poseStack,
                                              float partialTick,
                                              long nanoTime,
                                              boolean outOfWorld,
                                              Camera camera,
                                              GameRenderer gameRenderer,
                                              LightTexture lightTexture,
                                              Matrix4f projectionMatrix,
                                              CallbackInfo ci) {
        if (GlitchRuntime.depthChaos) {
            RenderSystem.enableDepthTest();
        }
    }
}
