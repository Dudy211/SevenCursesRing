package com.yuneko.sevencurses;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.InterModComms;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

import java.util.List;

/**
 * 通过 IMC 自动给玩家开「戒指」槽位。
 *
 * <p>⚠️ 关键发现（反汇编 CuriosSlotManager / CuriosConfig.Common / LegacySlotManager 得出）：
 * <ul>
 *   <li>{@code data/curios/curios/slots/*.json} <b>只是定义槽位类型</b>，不会给玩家开槽；</li>
 *   <li>Curios 给玩家开槽走的是 {@code CuriosConfig.Common.slots} 配置项，
 *       默认值是 <b>空列表</b> —— 所以光放 json 文件，玩家身上不会出现槽位；</li>
 *   <li>正确做法是用 <b>IMC 消息</b> {@code SlotTypeMessage.REGISTER_TYPE}。</li>
 * </ul>
 *
 * <p>★payload 类型（字节码实证）：{@code LegacySlotManager.lambda$processImc$4} 里是
 * <pre>
 *   Object msg = m.messageSupplier().get();
 *   if (msg instanceof SlotTypeMessage) { ... }
 *   else if (msg instanceof Iterable) { ... }   // 也支持一次发多个
 * </pre>
 * 所以 payload 必须是 {@link SlotTypeMessage} 对象（<b>不是 JSON 字符串</b>），
 * 用官方 {@link SlotTypeMessage.Builder} 构造。
 */
public final class CuriosSlotRegistration {

    private CuriosSlotRegistration() {
    }

    /**
     * 在 {@code InterModEnqueueEvent} 里调用。
     *
     * <p>{@code size=1} = 给玩家开 1 个戒指槽。
     */
    public static void enqueue() {
        SlotTypeMessage message = SlotTypePreset.RING.getMessageBuilder()
                .size(1)
                .build();

        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> message);

        SevenCursesMod.LOG.info("[SevenCurses] 已通过 IMC 发送 ring 槽位注册（size=1）。");
    }

    /** 备用：给别的槽位也能用（保留 Invariant 便于调试）。 */
    static SlotTypeMessage custom(String id, int size, String iconPath) {
        return new SlotTypeMessage.Builder(id)
                .size(size)
                .icon(new ResourceLocation("curios", iconPath))
                .priority(160)
                .build();
    }

    /** 一次发多个（Curios 支持 Iterable payload）。 */
    static List<SlotTypeMessage> batch() {
        return List.of(
                SlotTypePreset.RING.getMessageBuilder().size(1).build()
        );
    }
}
