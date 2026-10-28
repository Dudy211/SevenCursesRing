package com.yuneko.sevencurses;

/**
 * 服务端可见的故障状态（双端安全，不引用任何 client 类）。
 *
 * <p>为什么单独放一个类：{@code GlitchRuntime} 引用了 {@code Minecraft} 等 client 类，
 * 服务端不能加载它。但 {@code tps_spike} 需要<b>服务端 tick 线程</b>读得到（要在集成
 * 服务器上睡一下制造 mspt 飙升）—— 所以这个 flag 必须放在一个双端都能加载的类里。
 *
 * <p>v1.4.0 起这里同时是 <b>TPS 咒的自证面板</b>：
 * <ul>
 *   <li>{@link #tickSeen} —— {@code MinecraftServer.tickServer} 被调用的累计次数，
 *       Mixin 活着它就会一直涨。涨不动 = Mixin 没生效（而不是"效果不明显"）。</li>
 *   <li>{@link #sleptTicks} / {@link #sleptMs} —— 本次真正睡掉了多少 tick / 毫秒。</li>
 *   <li>{@link #avgPeriodMs} —— 实测的 tick 周期（含我们自己的睡眠），
 *       {@code /curse diag} 和 F3 都读它，用来显示<b>真的</b> mspt / TPS。</li>
 *   <li>{@link #eventFallback} —— Mixin 连续 10 tick 一次都没跑时的兜底通道开关。</li>
 * </ul>
 */
public final class ServerGlitchState {

    /** 是否正在做 TPS 抽风（服务端 tick 上睡一下）。 */
    public static volatile boolean tpsSpike = false;

    /* ---- 自证 / 诊断 ---- */

    /** tickServer 被调用总数。Mixin 生效 → 每 tick +1。 */
    public static volatile long tickSeen = 0L;

    /** 本次七咒里睡掉的 tick 数。 */
    public static volatile int sleptTicks = 0;

    /** 本次七咒里累计睡掉的毫秒。 */
    public static volatile long sleptMs = 0L;

    /** 实测 tick 周期（毫秒）；<=0 表示还没有样本。 */
    public static volatile double lastPeriodMs = -1.0;

    /** 实测 tick 周期的平滑值（EMA）。 */
    public static volatile double avgPeriodMs = -1.0;

    /** Mixin 不生效时的兜底：改在 ServerTickEvent 里睡。 */
    public static volatile boolean eventFallback = false;

    /** 上一次 tickServer 返回时的 nanoTime —— 只在服务端线程读写。 */
    public static volatile long lastReturnNs = 0L;

    private ServerGlitchState() {
    }

    /** 每次开始一轮「迟世」时清零。 */
    public static void resetRun() {
        sleptTicks = 0;
        sleptMs = 0L;
        lastPeriodMs = -1.0;
        avgPeriodMs = -1.0;
        eventFallback = false;
    }

    /** 记一次睡眠（Mixin 主通道和兜底通道都走这里）。 */
    public static void recordSleep(int ms) {
        sleptTicks++;
        sleptMs += ms;
    }

    /** 由实测周期推出来的 TPS；没有样本时按满速 20 返回。 */
    public static double tps() {
        double p = avgPeriodMs;
        if (p <= 0.0) {
            return 20.0;
        }
        return Math.min(20.0, 1000.0 / p);
    }
}
