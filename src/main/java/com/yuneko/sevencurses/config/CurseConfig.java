package com.yuneko.sevencurses.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 客户端配置：{@code config/sevencurses-client-v2.toml}。
 *
 * <p>所有值在运行期读取（不在构造期读），因为 config 加载晚于 mod 构造。
 * 读取前一律用 {@code CurseConfig.SPEC.isLoaded()} 兜底。
 */
public final class CurseConfig {

    public static final ForgeConfigSpec SPEC;

    /* ---- 总开关 ---- */
    public static final ForgeConfigSpec.BooleanValue GLOBAL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;

    /* ---- 「垂烬」：渲染故障调度 ---- */
    public static final ForgeConfigSpec.BooleanValue GLITCH_ENABLED;
    public static final ForgeConfigSpec.IntValue GLITCH_INTENSITY;
    public static final ForgeConfigSpec.IntValue GLITCH_MIN_INTERVAL;
    public static final ForgeConfigSpec.IntValue GLITCH_MAX_INTERVAL;
    public static final ForgeConfigSpec.IntValue GLITCH_DURATION_MIN;
    public static final ForgeConfigSpec.IntValue GLITCH_DURATION_MAX;
    public static final ForgeConfigSpec.IntValue GLITCH_COOLDOWN;

    /* ---- LIGHT 组 ---- */
    public static final ForgeConfigSpec.BooleanValue G_MISSING_TEXTURE;
    public static final ForgeConfigSpec.DoubleValue MISSING_CHANCE;
    public static final ForgeConfigSpec.BooleanValue G_UV_SCRAMBLE;
    public static final ForgeConfigSpec.DoubleValue UV_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue G_ARTIFACT_BLOCKS;

    /* ---- MEDIUM 组 ---- */
    public static final ForgeConfigSpec.BooleanValue G_DEPTH_CHAOS;
    public static final ForgeConfigSpec.BooleanValue G_COLOR_WARP;
    public static final ForgeConfigSpec.BooleanValue G_SIGNAL_LOSS;
    public static final ForgeConfigSpec.BooleanValue G_FPS_SPIKE;
    public static final ForgeConfigSpec.BooleanValue G_RENDER_DOWNGRADE;
    public static final ForgeConfigSpec.BooleanValue G_FRAME_LOCK;
    public static final ForgeConfigSpec.BooleanValue G_GAMMA_LOCK;
    public static final ForgeConfigSpec.BooleanValue G_MEMORY_PRESSURE;
    /** 占位比例（占 maxHeap 的百分比），默认 0.25。 */
    public static final ForgeConfigSpec.DoubleValue MEM_HOLD_FRACTION;
    /** 是否开搅动线程（持续制造垃圾 → 真 GC 卡顿）。 */
    public static final ForgeConfigSpec.BooleanValue MEM_CHURN;

    /* ---- HEAVY 组 ---- */
    public static final ForgeConfigSpec.BooleanValue G_GEOMETRY_WARP;
    public static final ForgeConfigSpec.BooleanValue G_CAMERA_CHAOS;
    public static final ForgeConfigSpec.BooleanValue G_FAKE_TDR;
    public static final ForgeConfigSpec.BooleanValue G_TPS_SPIKE;
    /** 每个 tick 最少睡多少毫秒（默认 60）。 */
    public static final ForgeConfigSpec.IntValue TPS_SLEEP_MIN;
    /** 每个 tick 最多睡多少毫秒（默认 200 → TPS 掉到 4~8）。 */
    public static final ForgeConfigSpec.IntValue TPS_SLEEP_MAX;
    public static final ForgeConfigSpec.BooleanValue G_MOUSE_FAILURE;

    /* ---- 「乱指」：键盘错位 ---- */
    public static final ForgeConfigSpec.IntValue KEY_BURST_MIN;
    public static final ForgeConfigSpec.IntValue KEY_BURST_MAX;
    public static final ForgeConfigSpec.IntValue KEY_REST_MIN;
    public static final ForgeConfigSpec.IntValue KEY_REST_MAX;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("七咒之戒 · 总开关").push("general");
        GLOBAL_ENABLED = b.comment("总开关。关掉后所有诅咒立即失效并恢复。")
                .define("enabled", true);
        HUD_ENABLED = b.comment("右下角显示「当前被什么咒诅咒中」。")
                .define("hud", true);
        b.pop();

        b.comment("垂烬 —— 随机渲染故障调度器").push("glitch");
        GLITCH_ENABLED = b.comment("渲染故障总开关。")
                .define("enabled", true);
        GLITCH_INTENSITY = b.comment("强度上限：0=只出 LIGHT 1=含 MEDIUM 2=含 HEAVY")
                .defineInRange("maxIntensity", 2, 0, 2);
        GLITCH_MIN_INTERVAL = b.comment("两次故障之间最短间隔（tick，20=1秒）。默认 10 = 0.5秒，主打一个密。")
                .defineInRange("minIntervalTicks", 10, 1, 72000);
        GLITCH_MAX_INTERVAL = b.comment("两次故障之间最长间隔（tick）。默认 50 = 2.5秒。")
                .defineInRange("maxIntervalTicks", 50, 1, 72000);
        GLITCH_DURATION_MIN = b.comment("单次故障最短持续时间（tick）。默认 40 = 2秒。")
                .defineInRange("minDurationTicks", 40, 1, 200);
        GLITCH_DURATION_MAX = b.comment("单次故障最长持续时间（tick）。默认 120 = 6秒；上限 400=20秒。个别故障还会按 durationScale 再拉长。")
                .defineInRange("maxDurationTicks", 120, 1, 400);
        GLITCH_COOLDOWN = b.comment("同一种故障的冷却（tick）。默认 60 = 3秒，让同一种别立刻连着来。")
                .defineInRange("cooldownTicks", 60, 0, 72000);

        b.comment("===== LIGHT：纯视觉，零风险 =====").push("light");

        b.comment("贴图遗失（方块/物品随机变成紫黑格子）").push("missing_texture");
        G_MISSING_TEXTURE = b.define("enabled", true);
        MISSING_CHANCE = b.comment("多少比例的贴图会变格子（0.35 = 35%）")
                .defineInRange("chance", 0.35D, 0.0D, 1.0D);
        b.pop();

        b.comment("经纬错织（纹理被拉成条纹 / 糊成色块）").push("uv_scramble");
        G_UV_SCRAMBLE = b.define("enabled", true);
        UV_AMOUNT = b.comment("UV 抖动幅度：0.1 轻微拉花，0.5 糊成一团")
                .defineInRange("amount", 0.25D, 0.0D, 2.0D);
        b.pop();

        b.comment("显存碎屑（屏幕随机出现显存坏点式的彩条）").push("artifact_blocks");
        G_ARTIFACT_BLOCKS = b.define("enabled", true);
        b.pop();

        b.pop();

        b.comment("===== MEDIUM：会动 GL 状态，但成对还原 =====").push("medium");

        b.comment("深浅失序（物体互相穿插 / 忽隐忽现）").push("depth_chaos");
        G_DEPTH_CHAOS = b.define("enabled", true);
        b.pop();

        b.comment("颜色叛变（世界被染成紫 / 绿 / 负片）").push("color_warp");
        G_COLOR_WARP = b.define("enabled", true);
        b.pop();

        b.comment("断信（画面被黑幕盖住约 2 秒；刻意做成保持而不是高频闪烁）").push("signal_loss");
        G_SIGNAL_LOSS = b.define("enabled", true);
        b.pop();

        b.comment("呼吸不匀（FPS 乱跳 / 偶尔卡一帧，走主线程 sleep）").push("fps_spike");
        G_FPS_SPIKE = b.define("enabled", true);
        b.pop();

        b.comment("褪色（渲染距离/云/阴影/AO/粒子全打到地板，GPU 占用真掉）").push("render_downgrade");
        G_RENDER_DOWNGRADE = b.define("enabled", true);
        b.pop();

        b.comment("停钟（framerateLimit 打到 5~15，画面像幻灯片）").push("frame_lock");
        G_FRAME_LOCK = b.define("enabled", true);
        b.pop();

        b.comment("沉夜（gamma=0 + 黑雾，世界几乎全黑）").push("gamma_lock");
        G_GAMMA_LOCK = b.define("enabled", true);
        b.pop();

        b.comment("淤积（安全版：占 25% 堆 + 搅动线程制造真 GC 卡顿，不会毁存档）").push("memory_pressure");
        G_MEMORY_PRESSURE = b.define("enabled", true);
        MEM_HOLD_FRACTION = b.comment(
                        "占位比例：吃掉 maxHeap 的多少。默认 0.25。",
                        "硬性保证：分配完必须还剩 25% 堆；占用被夹在 256MB ~ 3GB 之间。")
                .defineInRange("holdFraction", 0.25D, 0.0D, 0.45D);
        MEM_CHURN = b.comment(
                        "搅动：一条守护线程不停 new 4MB 数组只保留最近 12 个（≈48MB 活动集），",
                        "让 young GC 连绵不断 —— 这才是「锁内存」该有的手感。默认开。",
                        "关掉就只剩占位（F3 的 Mem 会高，但不卡）。")
                .define("churn", true);
        b.pop();

        b.pop();

        b.comment("===== HEAVY：矩阵 / 镜头 / 演出 =====").push("heavy");

        b.comment("空间拉伤（世界被拉伸 / 压扁 / 漂移）").push("geometry_warp");
        G_GEOMETRY_WARP = b.define("enabled", true);
        b.pop();

        b.comment("目眩（画面自己倾斜摇摆）").push("camera_chaos");
        G_CAMERA_CHAOS = b.define("enabled", true);
        b.pop();

        b.comment("假死（黑屏 + 仿 Windows「显示驱动程序已停止响应」对话框，纯 HUD 演出）").push("fake_tdr");
        G_FAKE_TDR = b.define("enabled", true);
        b.pop();

        b.comment("迟世（服务端 tick 上睡 → mspt 飙升 → 世界真卡，单机有效）").push("tps_spike");
        G_TPS_SPIKE = b.define("enabled", true);
        TPS_SLEEP_MIN = b.comment("每个 tick 最少睡多少毫秒。默认 60。")
                .defineInRange("minSleepMs", 60, 1, 900);
        TPS_SLEEP_MAX = b.comment(
                        "每个 tick 最多睡多少毫秒。默认 200 → 实测 TPS 掉到 4~8。",
                        "想更狠就调到 400（TPS ≈ 2~3），还能正常操作，但会明显掉帧感。")
                .defineInRange("maxSleepMs", 200, 1, 2000);
        b.pop();

        b.comment("失手（完全不动 or 反转，随机）").push("mouse_failure");
        G_MOUSE_FAILURE = b.define("enabled", true);
        b.pop();

        b.pop();
        b.pop();

        b.comment("乱指 —— 脉冲式，错位一阵子（好不容易习惯）再恢复正常（又得重新适应）").push("keyscramble");
        KEY_BURST_MIN = b.comment("错位持续时间下限（tick）。默认 300 = 15秒，够你开始适应。")
                .defineInRange("burstMinTicks", 300, 20, 72000);
        KEY_BURST_MAX = b.comment("错位持续时间上限（tick）。默认 600 = 30秒。")
                .defineInRange("burstMaxTicks", 600, 20, 72000);
        KEY_REST_MIN = b.comment("恢复正常时间下限（tick）。默认 60 = 3秒，短暂的喘息。")
                .defineInRange("restMinTicks", 60, 20, 72000);
        KEY_REST_MAX = b.comment("恢复正常时间上限（tick）。默认 160 = 8秒。")
                .defineInRange("restMaxTicks", 160, 20, 72000);
        b.pop();

        SPEC = b.build();
    }

    private CurseConfig() {
    }

    public static void register() {
        // ⚠️ 文件名带 v2 —— 强制生成新文件，避免旧 config（v0.1~v0.5 的老默认值）覆盖新默认值。
        // 旧文件 sevencurses-client.toml 会被忽略，可以手动删掉。
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "sevencurses-client-v2.toml");
    }
}
