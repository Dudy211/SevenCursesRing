package com.yuneko.sevencurses.glitch;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yuneko.sevencurses.ServerGlitchState;
import com.yuneko.sevencurses.glitch.impl.MemoryPressureGlitch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 渲染故障的「共享状态」。
 *
 * <p>Mixin 会在渲染热路径上高频读这里，所以这里：
 * <ul>
 *   <li>全部是 volatile 基本类型 / 零分配；</li>
 *   <li>判断逻辑全做成纯算术，不做对象创建、不加锁；</li>
 *   <li>「同一轮故障里同一个贴图表现一致」靠 seed 做确定性伪随机，
 *       换一轮 seed 就换一批坏掉的贴图 —— 比每次都随机更像个真故障。</li>
 * </ul>
 *
 * <p>⚠️ 安全性约定：这里只存「意图」。真正改 GL 状态的地方（{@code LevelRendererMixin}）
 * 必须成对 write/restore，并且 {@link #resetAll()} 要能把所有意图清干净。
 */
public final class GlitchRuntime {

    /* ---- LIGHT：贴图遗失（走「换绑 atlas 贴图」路线）----
       ⚠️ 关键认知：方块/物品的 UV 坐标在烘焙时就被烧进了四边形顶点数据，
          所以改 getSprite / getU0 在渲染时根本不会被读到 —— 那条路是死的。
          正确做法：在 setShaderTexture 把「方块图集」换成一张紫黑格子的占位贴图。 */
    public static volatile boolean missingTexture = false;
    /** 我们自己生成的紫黑缺失贴图，注册进 TextureManager。 */
    public static final ResourceLocation MISSING_TEX =
            new ResourceLocation("sevencurses", "missingno");
    public static volatile boolean texGuard = false;
    public static volatile boolean missingTexReady = false;

    /* ---- LIGHT：经纬错织（只对粒子 / GUI 精灵有效，方块是烘焙的吃不到）---- */
    public static volatile boolean uvScramble = false;
    public static volatile float uvAmount = 0.25f;

    /* ---- LIGHT：显存碎屑 ---- */
    public static volatile boolean artifacts = false;
    public static volatile int artifactCount = 0;
    public static volatile long artifactSeed = 0L;

    /* ---- MEDIUM：深浅失序 ---- */
    public static volatile boolean depthChaos = false;

    /* ---- MEDIUM：颜色叛变 ---- */
    public static volatile boolean colorWarp = false;
    public static volatile float warpR = 1.0f;
    public static volatile float warpG = 1.0f;
    public static volatile float warpB = 1.0f;
    /** 重入保护：拦截 setShaderColor 时自己不能再被拦一次。 */
    public static volatile boolean tintGuard = false;

    /* ---- MEDIUM：断信（全屏黑幕，非闪烁） ---- */
    public static volatile boolean signalLoss = false;
    public static volatile int signalLossAlpha = 0xE8;

    /* ---- MEDIUM：呼吸不匀（在客户端 tick 里随机睡一下） ---- */
    public static volatile boolean fpsSpike = false;

    /* ---- MEDIUM：沉夜（gamma=0 + 黑雾）---- */
    public static volatile boolean gammaDark = false;

    /* ---- MEDIUM：淤积（安全版，有界池 + 搅动线程）---- */
    public static volatile boolean memoryPressure = false;

    /* ---- HEAVY：失手 / 反转 ---- */
    public static volatile boolean mouseDead = false;
    public static volatile boolean mouseInvert = false;

    /* ---- HEAVY：空间拉伤 ---- */
    public static volatile boolean geometryWarp = false;
    public static volatile float geometryAmount = 0.12f;

    /* ---- HEAVY：目眩 ---- */
    public static volatile boolean cameraChaos = false;
    public static volatile float cameraRoll = 22.0f;
    public static volatile float cameraYaw = 9.0f;
    public static volatile float cameraPitch = 6.0f;

    /* ---- HEAVY：假死演出 ---- */
    public static volatile boolean fakeTdr = false;
    public static volatile long fakeTdrStartMs = 0L;

    /* ---- 本轮故障的随机种子 ---- */
    public static volatile int seed = 0;

    private GlitchRuntime() {
    }

    /** 确定性伪随机：同一个贴图在本轮故障里行为一致（32 位 mix 函数）。 */
    public static boolean rollMissing(ResourceLocation id) {
        int h = id.hashCode() ^ seed;
        h ^= (h >>> 16);
        h *= 0x7feb352d;
        h ^= (h >>> 15);
        h *= 0x846ca68b;
        h ^= (h >>> 16);
        return Math.floorMod(h, 100) < 35;
    }

    /** 给 UV 用的廉价抖动 —— 不做任何分配，可以安全地在逐顶点路径里跑。 */
    public static float jitter(Object self, int salt, float base) {
        int h = System.identityHashCode(self) * 0x9E3779B9
                + salt * 0x85EBCA6B
                + seed;
        h ^= (h >>> 15);
        h *= 0x2545F491;
        h ^= (h >>> 13);
        float t = (h & 0xFFFF) / 65535.0f;
        return base + (t - 0.5f) * 2.0f * uvAmount;
    }

    /**
     * 低频正弦振荡。
     *
     * <p>⚠️ 刻意用「慢速振荡」而不是「逐帧随机」：逐帧随机会变成高频闪烁 ——
     * 那既不像显卡故障，又有光敏性癫痫风险。慢速摆动看起来更像画面真的坏掉了。
     */
    public static float osc(float speed, float phase) {
        double t = (System.currentTimeMillis() % 600_000L) / 1000.0;
        return (float) Math.sin(t * speed + phase);
    }

    /** 是否是「方块/粒子图集」这种用 setShaderTexture 绑定的贴图 —— 这些才该被换掉。 */
    public static boolean isAtlas(ResourceLocation loc) {
        return loc != null && loc.getPath().contains("textures/atlas/");
    }

    /**
     * 在客户端线程上建一张「经典紫黑缺失贴图」并注册进 TextureManager（幂等）。
     *
     * <p>magenta = 0xFFFF00FF：对 R/B 通道对称，不管 NativeImage 是 RGBA 还是 ABGR
     * 字节序，画出来都是一样的洋红。黑色 0xFF000000 同理。
     */
    public static void ensureMissingTexture() {
        if (missingTexReady) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        int n = 16;
        NativeImage img = new NativeImage(n, n, false);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                // 8x8 大格 → 整张是 2x2 的洋红/黑棋盘
                boolean magenta = ((x / 8) + (y / 8)) % 2 == 0;
                img.setPixelRGBA(x, y, magenta ? 0xFFFF00FF : 0xFF000000);
            }
        }
        mc.getTextureManager().register(MISSING_TEX, new DynamicTexture(img));
        missingTexReady = true;
    }

    /** 一键清空 —— panic / 关开关时调用，保证不残留任何故障状态。 */
    public static void resetAll() {
        missingTexture = false;
        uvScramble = false;
        artifacts = false;
        artifactCount = 0;

        depthChaos = false;
        colorWarp = false;
        signalLoss = false;
        fpsSpike = false;
        gammaDark = false;
        // 内存那一路有线程和池子，必须走 hardStop 才能真收干净
        MemoryPressureGlitch.hardStop();
        mouseDead = false;
        mouseInvert = false;
        geometryWarp = false;
        cameraChaos = false;
        fakeTdr = false;
        ServerGlitchState.tpsSpike = false;

        // 保险：把可能残留的 GL 状态也还原一次
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } catch (Throwable ignored) {
            // 非 render 线程会抛，无害
        }
    }
}
