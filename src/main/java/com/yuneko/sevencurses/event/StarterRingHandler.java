package com.yuneko.sevencurses.event;

import com.yuneko.sevencurses.SevenCursesMod;
import com.yuneko.sevencurses.item.SevenCursesItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 开局发戒指。
 *
 * <p>第一次进世界时给玩家一个「七咒之戒」，放进背包。
 * 用玩家持久数据（{@code PlayerPersisted}）打标记，保证**只发一次**，
 * 死了/重登都不会再发。
 *
 * <p>⚠️ 双端安全：这个类只引用 ServerPlayer / ItemStack 等通用类，
 * 不碰任何 client 类，所以装在专用服务器上也没问题。
 */
@Mod.EventBusSubscriber(modid = SevenCursesMod.MODID)
public final class StarterRingHandler {

    /** 存在玩家持久 NBT 里的标记 key。 */
    private static final String GIVEN_TAG = "sevencurses:starter_ring_given";

    private StarterRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        giveOnce(player);
    }

    /** 首次重生时也检查一遍（覆盖"先死过一次再重登"的边缘情况）。 */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        giveOnce(player);
    }

    private static void giveOnce(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(GIVEN_TAG)) {
            return;
        }

        ItemStack ring = new ItemStack(SevenCursesItems.SEVEN_CURSES_RING.get());
        if (!player.getInventory().add(ring)) {
            // 背包满 → 掉在脚下（不丢东西）
            player.drop(ring, false);
        }
        data.putBoolean(GIVEN_TAG, true);

        SevenCursesMod.LOG.info("[SevenCurses] 已向 {} 发放七咒之戒。", player.getName().getString());
    }
}
