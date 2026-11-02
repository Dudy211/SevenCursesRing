package com.yuneko.sevencurses.boon;

/**
 * 「你的机器有多好」—— 七福全部按这个反向缩放。
 *
 * <p>★ 双端安全：这个类只存数字，<b>不引用任何 client 类</b>（不碰 GL、不碰 Minecraft）。
 * 真正读显卡 / 窗口像素的活由 {@code client.HardwareProbe} 干，它把结果写进这里；
 * 服务端逻辑只读这几个 int，读不到就用下面这套「中等偏上」的默认值。
 *
 * <p>为什么这么绕：七福里生命 / 伤害 / 护甲 / 移速 / 经验都是<b>服务端权威</b>的，
 * 必须在 ServerPlayer 上加 AttributeModifier 才有用；而硬件信息只有客户端拿得到。
 * 单机时两边同一个 JVM，静态字段天然共享 → 客户端探完，服务端直接读，
 * 完全不用发包。专门开服时服务端会退化成默认档（偏弱一点的补偿），可接受。
 */
public final class HardwareProfile {

    private static final int MIB = 1024 * 1024;

    /** Java 堆上限（MB）。玩家的 -Xmx，等价于「你给游戏多少内存」。 */
    public static volatile int heapMb = 4096;

    /** 逻辑核心数。 */
    public static volatile int cores = 4;

    /** 显卡档位 0~3（0=核显/很弱，3=很猛）。 */
    public static volatile int gpuTier = 1;

    /** 窗口像素总数（宽×高），默认 1080p。 */
    public static volatile int screenPixels = 1920 * 1080;

    /** 视距（区块），默认 12。 */
    public static volatile int viewDistance = 12;

    /**
     * 实测帧率（进世界后头两秒、<b>七咒还没开始压帧率时</b>测的），默认 60。
     *
     * <p>为什么不用「显示器刷新率」：那玩意儿对大多数人是个常量（60，顶多 144），
     * 七福就永远接近满值 = 等于没有。实测帧率才是"你这台机器跑这个游戏到底行不行"。
     */
    public static volatile double peakFps = 60.0;

    /** 是否已经测完（peakFps 冻结点）。 */
    public static volatile boolean fpsMeasured = false;

    /**
     * 实时帧率（1 秒一个窗口，EMA 平滑）。
     *
     * <p>跟 {@link #peakFps} 分工不同：peakFps 是"这台机器的能力上限"（冻结），
     * fpsEma 是"现在这一刻跑得怎么样"。七福「净」两者都用 —— 见 BoonService。
     */
    public static volatile double fpsEma = 0.0;

    /** 客户端是否已经探过一轮（服务端专用时恒为 false）。 */
    public static volatile boolean probed = false;

    private HardwareProfile() {
    }

    /** 读 JVM 层的两项（双端都能读）。idempotent。 */
    public static void probeJvm() {
        heapMb = (int) (Runtime.getRuntime().maxMemory() / MIB);
        cores = Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    /** 诊断串：给人看的一行。 */
    public static String describe() {
        return "heap=" + heapMb + "MB cores=" + cores + " gpuTier=" + gpuTier
                + " pixels=" + screenPixels + " viewDist=" + viewDistance
                + " fpsPeak=" + (peakFps > 0 ? String.format(java.util.Locale.ROOT, "%.0f", peakFps) : "?")
                + " fpsNow=" + (fpsEma > 0 ? String.format(java.util.Locale.ROOT, "%.0f", fpsEma) : "?")
                + (fpsMeasured ? "" : "(测帧中)")
                + (probed ? "" : " (未探测/服务端默认)");
    }
}
