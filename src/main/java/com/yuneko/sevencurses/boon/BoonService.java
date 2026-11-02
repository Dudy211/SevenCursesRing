package com.yuneko.sevencurses.boon;

import com.yuneko.sevencurses.config.CurseConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 七福 —— 与七咒一体两面，<b>只作用于戴着戒指的那个人</b>。
 *
 * <p>核心规则一条：<b>机器越好，福越弱；机器越烂，福越强。</b>
 * 七咒惩罚所有人，七福只补偿穷人。
 *
 * <h3>怎么知道「还戴着」</h3>
 * 不查 Curios 的槽位（那要额外 API 调用，还容易踩版本差异），
 * 而是用一次心跳：玩家的 {@code curioTick} 每 tick 会调 {@link #seen(ServerPlayer)}，
 * 打上时间戳；服务端每 20 tick 扫一遍，超过 60 tick（3 秒）没心跳 = 摘下了 → 清掉。
 * 摘下来的手感是「三秒后属性回正常」，而不是瞬间掉血。
 *
 * <h3>为什么全在服务端做</h3>
 * 生命 / 伤害 / 护甲 / 移速 / 经验都是<b>服务端权威</b>。客户端改只会让面板好看、
 * 实际不生效。单机时集成服务器就是这个 JVM，所以纯客户端模组也能生效；
 * 进别人的服务器时，这些属性会被对方服务端顶回去（视觉/输入类七咒不受影响）。
 */
public final class BoonService {

    /* ---- 每条福一个固定 UUID，方便反复覆盖而不叠加 ---- */
    private static final UUID UID_HEALTH = UUID.fromString("7c1d2a30-4b5e-4f60-8a71-0000000000b1");
    private static final UUID UID_DAMAGE = UUID.fromString("7c1d2a30-4b5e-4f60-8a71-0000000000b2");
    private static final UUID UID_ARMOR = UUID.fromString("7c1d2a30-4b5e-4f60-8a71-0000000000b3");
    private static final UUID UID_SPEED = UUID.fromString("7c1d2a30-4b5e-4f60-8a71-0000000000b4");

    private static final String MOD_NAME = "sevencurses.boon";

    /** 戴上戒指的「心跳」：UUID → 最后见到时的 gameTime。 */
    private static final Map<UUID, Long> SEEN = new ConcurrentHashMap<>();

    /** 心跳容忍窗口（tick）。20 tick = 1 秒。 */
    private static final int SEEN_WINDOW = 60;

    private BoonService() {
    }

    /* ================================================================== */
    /* 心跳                                                                */
    /* ================================================================== */

    /** 由戒指的 curioTick 调用（服务端侧）—— 记录「这个玩家还戴着」。 */
    public static void seen(ServerPlayer player) {
        if (!enabled()) {
            return;
        }
        SEEN.put(player.getUUID(), player.level().getGameTime());
    }

    public static void forget(ServerPlayer player) {
        SEEN.remove(player.getUUID());
        clear(player);
    }

    private static boolean stillWorn(ServerPlayer player) {
        Long last = SEEN.get(player.getUUID());
        if (last == null) {
            return false;
        }
        return player.level().getGameTime() - last <= SEEN_WINDOW;
    }

    /* ================================================================== */
    /* 主循环                                                              */
    /* ================================================================== */

    /** 服务端每 20 tick 调一次：戴着就套上，摘了就撤掉。 */
    public static void refresh(ServerPlayer player) {
        if (!enabled() || !stillWorn(player)) {
            clear(player);
            return;
        }
        apply(player);
    }

    public static void apply(ServerPlayer player) {
        int level = player.experienceLevel;
        double gate = gate(level);

        // 熄 · 生命上限 —— 内存越大血越少
        set(player, Attributes.MAX_HEALTH, UID_HEALTH, (healthMult() - 1.0) * gate,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        // 稳 · 攻击伤害 —— CPU 核心越多伤害越低
        set(player, Attributes.ATTACK_DAMAGE, UID_DAMAGE, (damageMult() - 1.0) * gate,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        // 远 · 移动速度 —— 视距越大跑得越慢
        set(player, Attributes.MOVEMENT_SPEED, UID_SPEED, (speedMult() - 1.0) * gate,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        // 净 · 护甲 —— 模组越多皮越薄
        set(player, Attributes.ARMOR, UID_ARMOR, armorAdd(level) * gate,
                AttributeModifier.Operation.ADDITION);

        // 生命上限可能被拉低到当前血量以下 —— 自己夹一下，别让界面显示「30/30 但实际 20」
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void clear(ServerPlayer player) {
        remove(player, Attributes.MAX_HEALTH, UID_HEALTH);
        remove(player, Attributes.ATTACK_DAMAGE, UID_DAMAGE);
        remove(player, Attributes.MOVEMENT_SPEED, UID_SPEED);
        remove(player, Attributes.ARMOR, UID_ARMOR);
    }

    /* ================================================================== */
    /* 数值（全部可被配置覆盖，方便在强机上测）                              */
    /* ================================================================== */

    /** 谦 · 总阀门：等级越高，七福整体越弱。0 级 = 全效。 */
    public static double gate(int experienceLevel) {
        double decay = 0.03D;
        try {
            if (CurseConfig.SPEC.isLoaded()) {
                decay = CurseConfig.BOON_LEVEL_DECAY.get();
            }
        } catch (Throwable ignored) {
            // 用默认值
        }
        return clamp(1.0D - experienceLevel * decay, 0.0D, 1.0D);
    }

    /** 熄：生命倍率。堆 2G→×1.5，4G→×1.0，8G→×0.5。 */
    public static double healthMult() {
        return clamp(1.0D + (4096D - heapMb()) / 4096D, 0.5D, 2.5D);
    }

    /**
     * 缓：帧率上限。<b>连续曲线，不是查表</b> —— 这样多出来的档位也有答案。
     *
     * <pre>
     *   cap = clamp( 10 + 230 x (1 - tier/3)^1.4 , 10, 240 )
     *
     *   tier 0  核显 / 软件渲染     → 240  （≈不限制）
     *   tier 1  入门独显            → 140
     *   tier 2  中端                →  58
     *   tier 3  高端                →  10  ← 最好的显卡就该被压到 10fps
     *   tier 4+ （假设的未来档）     → 夹到 10，不会再低
     * </pre>
     */
    public static int frameCap(int experienceLevel) {
        double s = clamp(gpuTier() / 3.0D, 0.0D, 1.0D);
        int base = (int) Math.round(10.0D + 230.0D * Math.pow(1.0D - s, 1.4D));
        base = (int) clamp(base, 10, 240);
        double g = gate(experienceLevel);
        return (int) Math.round(base + (240 - base) * (1.0D - g));
    }

    /**
     * 稳：伤害倍率。以 8 核为基准的<b>幂律</b>，不是线性表 —— 外推有定义。
     *
     * <pre>
     *   dmg = clamp( (8 / cores)^0.8 , 0.5, 6.0 )
     *
     *     1 核 → x6.0（触顶）     2 核 → x3.03
     *     4 核 → x1.74            6 核 → x1.26
     *     8 核 → x1.00（基准）   12 核 → x0.72
     *    16 核 → x0.57           32 核 → x0.33 → 触底 x0.50
     *    64 核 → 触底 x0.50
     * </pre>
     */
    public static double damageMult() {
        double base = Math.pow(8.0D / Math.max(1, cores()), 0.8D);
        return clamp(base, 0.5D, 6.0D);
    }

    /** 远：移速倍率。视距 4→×1.47，12→×1.22，32→×0.9。 */
    public static double speedMult() {
        return clamp(1.6D - viewDistance() / 32.0D, 0.9D, 1.5D);
    }

    /**
     * 净：护甲点数。<b>判据 = 实测帧率</b>（进世界头两秒、还没被七咒压帧率时量的）。
     *
     * <p>前两版用过「模组数」和「显示器刷新率」，都同一个毛病：
     * 对大多数人是个<b>常量</b>（模组固定、屏幕 60Hz 或 144Hz），
     * 七福于是永远停在接近满值 —— 等于没生效。实测帧率是真的会动，
     * 而且是"你这台机器跑这个游戏到底行不行"的唯一硬指标。
     *
     * <p>★ 它<b>可以倒扣</b>：超过 120fps 反而开始扣护甲 —— 跑得太顺的人不配挨揍。
     *
     * <pre>
     *   armor = clamp( 10 - (fps - 40) / 8 , -12, 12 )
     *
     *      30 fps → +11.3      45 fps →  +9.4
     *      60 fps →  +7.5      90 fps →  +3.8
     *     100 fps →  +2.5     120 fps →   0    ← 分界线
     *     144 fps →  -3.0     165 fps →  -5.6
     *     240 fps → -15 → 触底 -12
     * </pre>
     *
     * <p>输入是 {@link #effectiveFps(int)}：实时帧率，但「被我们自己的上限压着」时
     * 退回能力值 —— 否则「缓」把顶卡压到 10fps，会让所有人都拿满护甲。
     */
    public static double armorAdd(int experienceLevel) {
        return clamp(10.0D - (effectiveFps(experienceLevel) - 40.0D) / 8.0D, -12.0D, 12.0D);
    }

    /**
     * 净的判据帧率 —— <b>实时值，但防自己被自己骗</b>。
     *
     * <p>问题：顶卡戴上戒指后，「缓」会把它压到 10fps。如果直接拿实时帧率算，
     * 就会得到「你这机器只有 10 帧 → 给满护甲 +11」，于是<i>所有人</i>都是满甲，
     * 判据又退化成常量了。
     *
     * <p>所以：
     * <ul>
     *   <li>当前帧率 <b>贴着我们的上限</b>（±15%）且明显低于能力值 → 说明是我们在压，
     *       改用<b>能力值</b>（进世界头两秒量的、已冻结）来算；</li>
     *   <li>否则说明是场景真的卡（打雷 / 大爆炸 / 一堆实体）→ 用<b>实时值</b>给补偿。
     *       「世界越吃力，你越抗揍」。</li>
     * </ul>
     */
    private static double effectiveFps(int experienceLevel) {
        int override = override("fps");
        if (override > 0) {
            return override;
        }
        double capability = HardwareProfile.peakFps;
        double now = HardwareProfile.fpsEma;
        if (now <= 0.0D) {
            return capability;
        }
        double cap = frameCap(experienceLevel);
        double tolerance = Math.max(3.0D, cap * 0.15D);
        boolean cappedByUs = Math.abs(now - cap) <= tolerance && capability > now + 5.0D;
        return cappedByUs ? capability : now;
    }

    /** 明：经验倍率。1080p→×1.0，4K→×0.4，720p→×1.33。 */
    public static double xpMult(int experienceLevel) {
        double raw = clamp(1.6D - pixels() / 2_073_600.0D * 0.6D, 0.4D, 1.6D);
        return 1.0D + (raw - 1.0D) * gate(experienceLevel);
    }

    /* ================================================================== */
    /* 硬件取值（带配置覆盖）                                              */
    /* ================================================================== */

    private static boolean enabled() {
        try {
            return !CurseConfig.SPEC.isLoaded() || CurseConfig.BOON_ENABLED.get();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static int heapMb() {
        HardwareProfile.probeJvm();
        int override = override("heap");
        return override > 0 ? override : HardwareProfile.heapMb;
    }

    private static int cores() {
        HardwareProfile.probeJvm();
        int override = override("cores");
        return override > 0 ? override : HardwareProfile.cores;
    }

    private static int gpuTier() {
        int override = override("gpu");
        return override >= 0 ? override : HardwareProfile.gpuTier;
    }

    private static int pixels() {
        return HardwareProfile.screenPixels;
    }

    private static int viewDistance() {
        return HardwareProfile.viewDistance;
    }

    /** -1 = 不覆盖。 */
    private static int override(String key) {
        try {
            if (!CurseConfig.SPEC.isLoaded()) {
                return -1;
            }
            return switch (key) {
                case "heap" -> CurseConfig.BOON_OVERRIDE_HEAP_MB.get();
                case "cores" -> CurseConfig.BOON_OVERRIDE_CORES.get();
                case "gpu" -> CurseConfig.BOON_OVERRIDE_GPU_TIER.get();
                case "fps" -> CurseConfig.BOON_OVERRIDE_FPS.get();
                default -> -1;
            };
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /* ================================================================== */
    /* 属性读写                                                            */
    /* ================================================================== */

    private static void set(ServerPlayer player, Attribute attribute, UUID id,
                            double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);            // 先撤旧的，避免每轮叠加
        if (Math.abs(amount) > 1.0E-4) {
            // transient：不写进 NBT，退出/重生后自动消失，下一轮 refresh 再套上
            inst.addTransientModifier(new AttributeModifier(id, MOD_NAME, amount, op));
        }
    }

    private static void remove(ServerPlayer player, Attribute attribute, UUID id) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(id);
        }
    }

    /* ================================================================== */
    /* 诊断                                                                */
    /* ================================================================== */

    /** 给 {@code /curse diag} 用：一眼看清七福现在各是多少。 */
    public static String describe(ServerPlayer player) {
        int lvl = player == null ? 0 : player.experienceLevel;
        return String.format(java.util.Locale.ROOT,
                "生命×%.2f  帧率上限%d  伤害×%.2f  移速×%.2f  护甲+%.1f  经验×%.2f  阀门%.2f(等级%d)",
                healthMult(), frameCap(lvl), damageMult(), speedMult(),
                armorAdd(lvl), xpMult(lvl), gate(lvl), lvl);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
