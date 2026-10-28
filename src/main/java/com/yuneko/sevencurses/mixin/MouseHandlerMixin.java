package com.yuneko.sevencurses.mixin;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「失手」—— 鼠标失灵 / 反转。
 *
 * <p>字节码实证：{@code turnPlayer()} 直接 {@code getfield accumulatedDX} 读位移
 * （不走 getXVelocity），而且结束后会 {@code putfield} reset。
 * 所以：
 * <ul>
 *   <li>「失灵」= HEAD cancel + 清零 accumulated → 鼠标不动，不堆积</li>
 *   <li>「反转」= HEAD 翻转 accumulated 符号，不 cancel → 让原版用反转后的值继续跑，跑完自己 reset</li>
 * </ul>
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "turnPlayer()V", at = @At("HEAD"), cancellable = true)
    private void sevencurses$mouseCurse(CallbackInfo ci) {
        MouseHandlerAccessor self = (MouseHandlerAccessor) (Object) this;

        if (GlitchRuntime.mouseDead) {
            // 清掉累积位移，否则结束后会猛转一下
            self.setAccumulatedDX(0);
            self.setAccumulatedDY(0);
            ci.cancel();
        } else if (GlitchRuntime.mouseInvert) {
            // 翻转符号 → 往上变往下、往左变往右；不 cancel，让原版用反转值跑完再自己 reset
            self.setAccumulatedDX(-self.getAccumulatedDX());
            self.setAccumulatedDY(-self.getAccumulatedDY());
        }
    }
}
