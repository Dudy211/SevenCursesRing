package com.yuneko.sevencurses.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 {@link MouseHandler} 的私有字段 {@code accumulatedDX/DY}。
 *
 * <p>用 @Accessor（不是 @Shadow）—— 因为 @Accessor 会被 Mixin AP 写进 refmap，
 * 生产环境自动映射到 SRG 字段名；@Shadow 字段不会被写进 refmap，会找不到。
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor("accumulatedDX")
    double getAccumulatedDX();

    @Accessor("accumulatedDX")
    void setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    double getAccumulatedDY();

    @Accessor("accumulatedDY")
    void setAccumulatedDY(double value);
}
