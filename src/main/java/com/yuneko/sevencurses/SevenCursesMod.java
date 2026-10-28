package com.yuneko.sevencurses;

import com.mojang.logging.LogUtils;
import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.item.SevenCursesItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 七咒之戒 —— Forge 1.20.1。七咒全部实现（v1.4.0）。
 *
 * <p>TPS 抽风改走 Mixin {@code MinecraftServer.tickServer}（比事件监听靠谱）。
 * 锁内存做了安全版（有界池 + 搅动线程）。
 */
@Mod(SevenCursesMod.MODID)
public class SevenCursesMod {

    public static final String MODID = "sevencurses";
    public static final Logger LOG = LogUtils.getLogger();

    public SevenCursesMod() {
        CurseConfig.register();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SevenCursesItems.register(modBus);
        modBus.addListener(SevenCursesMod::onCommonSetup);
        // ★ 通过 IMC 自动注册戒指槽位（必须走 InterModEnqueueEvent）
        modBus.addListener(SevenCursesMod::onInterModEnqueue);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.yuneko.sevencurses.client.ClientBootstrap.init(modBus);
        }

        LOG.info("[SevenCurses] 七咒之戒已装载（v1.4.0：迟世带自证心跳 + 兜底通道，淤积改占位+搅动）。");
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CuriosApi.registerCurio(
                SevenCursesItems.SEVEN_CURSES_RING.get(),
                SevenCursesItems.SEVEN_CURSES_RING.get()));
    }

    /**
     * ★ 自动给玩家开戒指槽位。
     *
     * <p>必须在 {@code InterModEnqueueEvent} 里发 —— Curios 在
     * {@code InterModProcessEvent} 收，这个顺序由 FML 保证。
     */
    private static void onInterModEnqueue(InterModEnqueueEvent event) {
        CuriosSlotRegistration.enqueue();
    }
}
