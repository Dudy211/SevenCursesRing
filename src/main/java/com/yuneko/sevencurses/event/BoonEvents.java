package com.yuneko.sevencurses.event;

import com.yuneko.sevencurses.SevenCursesMod;
import com.yuneko.sevencurses.boon.BoonService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 七福的服务端接线。
 *
 * <p>只用两个事件，够用且不碰版本差异：
 * <ul>
 *   <li>{@link TickEvent.PlayerTickEvent} —— 每 20 tick 扫一次「还戴着吗」，
 *       戴着就套属性，摘了就撤。</li>
 *   <li>{@link PlayerXpEvent.XpChange} —— 「明」：按分辨率缩放经验获取。</li>
 * </ul>
 *
 * <p>⚠️ 双端安全：这里只引用 ServerPlayer / 事件类，不碰 client。
 */
@Mod.EventBusSubscriber(modid = SevenCursesMod.MODID)
public final class BoonEvents {

    /** 扫频间隔（tick）。20 = 1 秒一次，开销可以忽略。 */
    private static final int SCAN_INTERVAL = 20;

    private BoonEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % SCAN_INTERVAL != 0) {
            return;
        }
        BoonService.refresh(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BoonService.forget(player);
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double mult = BoonService.xpMult(player.experienceLevel);
        if (Math.abs(mult - 1.0D) < 1.0E-4) {
            return;
        }
        event.setAmount((int) Math.round(event.getAmount() * mult));
    }
}
