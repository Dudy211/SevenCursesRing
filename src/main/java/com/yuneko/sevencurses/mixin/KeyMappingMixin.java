package com.yuneko.sevencurses.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.yuneko.sevencurses.curse.KeyScrambleRuntime;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「乱指」—— 键盘错位。
 *
 * <p>注入点（字节码实证）：{@code KeyboardHandler.keyPress} 收到物理按键后依次调用
 * <pre>
 *   KeyMapping.set(key, action != GLFW_RELEASE);
 *   if (action == GLFW_PRESS) KeyMapping.click(key);
 * </pre>
 * 所以在<b>这两个 static 方法的 HEAD</b> 把物理键换成目标键再放行，
 * 下游的 {@code isDown()} / {@code consumeClick()} 就全跟着错了 ——
 * 一处注入覆盖全部按键逻辑，不用碰 KeyMapping 的任何字段。
 *
 * <p>重入保护走 {@link KeyScrambleRuntime#guard}：我们自己调 {@code KeyMapping.set}
 * 时不能再被拦一次，否则会无限递归。
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {

    @Inject(
            method = "set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sevencurses$scrambleSet(InputConstants.Key key, boolean held, CallbackInfo ci) {
        if (KeyScrambleRuntime.guard || !KeyScrambleRuntime.active) {
            return;
        }
        InputConstants.Key mapped = KeyScrambleRuntime.remap(key);
        if (mapped == key) {
            return;
        }
        if (mapped == null) {
            // DEAD 模式：直接吞掉，让这个键"永远没被按过"
            ci.cancel();
            return;
        }
        KeyScrambleRuntime.guard = true;
        try {
            KeyMapping.set(mapped, held);
        } finally {
            KeyScrambleRuntime.guard = false;
        }
        ci.cancel();
    }

    @Inject(
            method = "click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sevencurses$scrambleClick(InputConstants.Key key, CallbackInfo ci) {
        if (KeyScrambleRuntime.guard || !KeyScrambleRuntime.active) {
            return;
        }
        InputConstants.Key mapped = KeyScrambleRuntime.remap(key);
        if (mapped == key) {
            return;
        }
        if (mapped == null) {
            ci.cancel();
            return;
        }
        KeyScrambleRuntime.guard = true;
        try {
            KeyMapping.click(mapped);
        } finally {
            KeyScrambleRuntime.guard = false;
        }
        ci.cancel();
    }
}
