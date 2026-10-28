package com.yuneko.sevencurses.item;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import com.yuneko.sevencurses.curse.CurseType;

/**
 * 七咒之戒 —— Curios 饰品。
 *
 * <p>戴上即中咒（GPU + 键盘错位自动强制开启），<b>只有创造模式能摘下</b>。
 *
 * <p>文案排版：先说物（它凉得像…），再说听起来像什么（听见玻璃裂），
 * 然后才是那七行刻字 —— <b>每咒两行</b>：上面雅名 + 铭文，下面一行真实作用。
 * 文学只管好看，作用一点不含糊。
 *
 * <p>⚠️ 双端安全：这个类是<b>双端加载</b>的，通过 {@code static Consumer} 回调
 * 把 curioTick 接到客户端的 CurseManager，不直接引用任何 client 类。
 */
public class SevenCursesRingItem extends Item implements ICurioItem {

    public static Consumer<LivingEntity> curioTickHandler = wearer -> {};

    public SevenCursesRingItem(Properties props) {
        super(props);
    }

    @Override
    public boolean canUnequip(SlotContext ctx, ItemStack stack) {
        return ctx.getWearer() instanceof Player player && player.isCreative();
    }

    @Override
    public boolean canRightClickEquip(ItemStack stack) {
        return true;
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        curioTickHandler.accept(ctx.getWearer());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 一、物本身
        tooltip.add(Component.translatable("item.sevencurses.seven_curses_ring.desc")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.sevencurses.seven_curses_ring.desc2")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        tooltip.add(Component.empty());

        // 二、刻字
        tooltip.add(Component.translatable("item.sevencurses.seven_curses_ring.curses")
                .withStyle(ChatFormatting.GOLD));

        for (CurseType type : CurseType.values()) {
            // 第一行：刻字（雅名 + 铭文）
            var line = Component.literal("  ")
                    .append(Component.translatable(type.translationKey())
                            .withStyle(type.implemented() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(type.translationKey() + ".desc")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            tooltip.add(line);

            // 第二行：这条咒到底干了什么 —— 说出来，别让人猜
            tooltip.add(Component.literal("     ")
                    .append(Component.translatable(type.translationKey() + ".effect")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }

        tooltip.add(Component.empty());

        // 三、规矩：摘不下来
        tooltip.add(Component.translatable("item.sevencurses.seven_curses_ring.tooltip2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
