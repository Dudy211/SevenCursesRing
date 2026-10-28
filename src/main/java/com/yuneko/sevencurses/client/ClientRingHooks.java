package com.yuneko.sevencurses.client;

import com.yuneko.sevencurses.curse.CurseManager;
import com.yuneko.sevencurses.item.SevenCursesRingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 把"七咒之戒"的 curioTick 回调接到客户端的 CurseManager。
 *
 * <p>这是物品类（双端）和 CurseManager（客户端）之间的桥 —— 物品类只持有一个
 * {@code static Consumer}，这里在客户端启动时把它装上。专用服务器不加载这个类，
 * 物品的回调就保持默认空操作，全程不碰 client 类。
 */
public final class ClientRingHooks {

    private ClientRingHooks() {
    }

    public static void install() {
        SevenCursesRingItem.curioTickHandler = ClientRingHooks::onCurioTick;
    }

    private static void onCurioTick(LivingEntity wearer) {
        if (!(wearer instanceof Player player)) {
            return;
        }
        if (player.level() == null || !player.level().isClientSide) {
            return;
        }
        // 只对本地玩家生效 —— 别的玩家的饰品 tick 也会触发，但那不是我们要管的
        if (player != Minecraft.getInstance().player) {
            return;
        }
        CurseManager.get().ringCurioTick();
    }
}
