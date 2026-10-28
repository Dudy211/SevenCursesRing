package com.yuneko.sevencurses.item;

import com.yuneko.sevencurses.SevenCursesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品 + 创造模式物品栏注册（双端）。
 */
public final class SevenCursesItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, SevenCursesMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SevenCursesMod.MODID);

    public static final RegistryObject<SevenCursesRingItem> SEVEN_CURSES_RING =
            ITEMS.register("seven_curses_ring",
                    () -> new SevenCursesRingItem(new Item.Properties()
                            .rarity(Rarity.EPIC)
                            .stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("seven_curses",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sevencurses"))
                    .icon(() -> new ItemStack(SEVEN_CURSES_RING.get()))
                    .displayItems((params, output) -> output.accept(SEVEN_CURSES_RING.get()))
                    .build());

    private SevenCursesItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
