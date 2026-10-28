package com.yuneko.sevencurses.curse;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 第 4 咒 · 乱指 —— <b>全键随机打乱，只豁免 ESC</b>。
 *
 * <p>每次错位开始时，把 {@code Options.keyMappings} 里所有键盘绑定（KEYSYM）
 * 随机洗成一个置换表：物理键 A → 物理键 B。
 * <b>只有 ESC 不动</b> —— 玩家至少还能打开暂停菜单退出 / 切创造。
 *
 * <p>一轮错位持续十几二十秒（由 config 控制），玩家好不容易适应了这套乱序，
 * 然后恢复正常 —— 下一轮又换一套新的乱序，得重新适应。这才是"诅咒"。
 *
 * <p>原理：{@code KeyboardHandler} 收到物理按键后调 {@code KeyMapping.set/click}，
 * 我们在 {@link com.yuneko.sevencurses.mixin.KeyMappingMixin} HEAD 把物理键换成
 * 目标键再放行。一处注入覆盖全部按键逻辑。
 */
public final class KeyScrambleRuntime {

    /** 是否正在错位。 */
    public static volatile boolean active = false;

    /** 重入保护：我们自己调 {@code KeyMapping.set} 时不要再被拦一次。 */
    public static volatile boolean guard = false;

    private static final Random RANDOM = new Random();

    /** 置换表：mapFrom[i] 的物理键 → mapTo[i] 的物理键。 */
    private static volatile KeyMapping[] mapFrom = new KeyMapping[0];
    private static volatile KeyMapping[] mapTo = new KeyMapping[0];

    private KeyScrambleRuntime() {
    }

    public static void begin() {
        buildScramble();
        active = true;
    }

    public static void end() {
        active = false;
        // ⚠️ 必须清一次键位状态，否则会留下「卡键」：
        //    玩家按住某键时若错位恰好结束，后面松开的释放事件不再被重定向 →
        //    目标键会一直停留在按下状态。收尾统一清掉所有被错位过的键。
        clearAllScrambledKeys();
    }

    /**
     * 把物理键映射成「实际生效的键」。
     *
     * @return 换过以后的键；{@code key} 本身表示不换（比如 ESC，不在置换表里）。
     */
    public static InputConstants.Key remap(InputConstants.Key key) {
        if (!active || key == null) {
            return key;
        }
        KeyMapping[] from = mapFrom;
        KeyMapping[] to = mapTo;
        for (int i = 0; i < from.length; i++) {
            if (from[i].getKey().equals(key)) {
                return to[i].getKey();
            }
        }
        return key;
    }

    /* ------------------------------------------------------------------ */

    private static void buildScramble() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            mapFrom = new KeyMapping[0];
            mapTo = new KeyMapping[0];
            return;
        }
        Options options = mc.options;

        List<KeyMapping> pool = new ArrayList<>();
        for (KeyMapping km : options.keyMappings) {
            InputConstants.Key k = km.getKey();
            // 只动键盘键（KEYSYM）；跳过鼠标键、未绑定的、以及 ESC
            if (k.getType() != InputConstants.Type.KEYSYM) {
                continue;
            }
            if (k.getValue() < 0) {
                continue;
            }
            if (isExempt(k)) {
                continue;
            }
            pool.add(km);
        }

        List<KeyMapping> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);
        // 尽量避免自映射（某键恰好换到自己 → 等于没换）
        for (int i = 0; i < shuffled.size() && shuffled.size() > 1; i++) {
            if (shuffled.get(i) == pool.get(i)) {
                int j = (i + 1) % shuffled.size();
                Collections.swap(shuffled, i, j);
            }
        }

        mapFrom = pool.toArray(new KeyMapping[0]);
        mapTo = shuffled.toArray(new KeyMapping[0]);
    }

    /** ★只豁免 ESC —— 玩家至少能打开暂停菜单。其他所有键都打乱。 */
    private static boolean isExempt(InputConstants.Key k) {
        return k.getType() == InputConstants.Type.KEYSYM
                && k.getValue() == GLFW.GLFW_KEY_ESCAPE;
    }

    /** 结束时：把所有被错位过的键强制松开，避免卡键。 */
    private static void clearAllScrambledKeys() {
        guard = true;
        try {
            for (KeyMapping km : mapFrom) {
                KeyMapping.set(km.getKey(), false);
            }
        } finally {
            guard = false;
        }
    }
}
