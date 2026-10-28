package com.yuneko.sevencurses.glitch;

import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.glitch.impl.ArtifactBlocksGlitch;
import com.yuneko.sevencurses.glitch.impl.CameraChaosGlitch;
import com.yuneko.sevencurses.glitch.impl.ColorWarpGlitch;
import com.yuneko.sevencurses.glitch.impl.DepthChaosGlitch;
import com.yuneko.sevencurses.glitch.impl.FakeTdrGlitch;
import com.yuneko.sevencurses.glitch.impl.FrameLockGlitch;
import com.yuneko.sevencurses.glitch.impl.FpsSpikeGlitch;
import com.yuneko.sevencurses.glitch.impl.GammaLockGlitch;
import com.yuneko.sevencurses.glitch.impl.GeometryWarpGlitch;
import com.yuneko.sevencurses.glitch.impl.MemoryPressureGlitch;
import com.yuneko.sevencurses.glitch.impl.MissingTextureGlitch;
import com.yuneko.sevencurses.glitch.impl.MouseFailureGlitch;
import com.yuneko.sevencurses.glitch.impl.RenderDowngradeGlitch;
import com.yuneko.sevencurses.glitch.impl.SignalLossGlitch;
import com.yuneko.sevencurses.glitch.impl.TpsSpikeGlitch;
import com.yuneko.sevencurses.glitch.impl.UvScrambleGlitch;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 渲染故障调度器。
 *
 * <p>状态机：
 * <pre>
 *   WAIT(waitTicks) --到点--> PLAY(current, remaining) --到点--> WAIT
 * </pre>
 *
 * <p>自保规则（硬性）：
 * <ul>
 *   <li>同一时刻只播一个故障；</li>
 *   <li>单次时长被 config 硬上限夹住（默认 2~6 秒，上限 400 tick = 20 秒）；</li>
 *   <li>同一种故障进冷却表，避免连着来两轮；</li>
 *   <li>强度过滤：{@code maxIntensity} 以下的才会进候选池；</li>
 *   <li>{@link #stopAll()} 一定调用 {@code current.stop()} + {@link GlitchRuntime#resetAll()}。</li>
 * </ul>
 */
public final class RenderGlitchScheduler {

    private final List<RenderGlitch> all = new ArrayList<>();
    private final Map<String, Integer> cooldown = new HashMap<>();
    private final RandomSource random = RandomSource.create();

    private RenderGlitch current;
    private int remaining;
    private int waitTicks;

    public RenderGlitchScheduler() {
        // LIGHT —— 纯视觉
        all.add(new MissingTextureGlitch());
        all.add(new UvScrambleGlitch());
        all.add(new ArtifactBlocksGlitch());
        // MEDIUM —— 动 GL 状态 / 黑幕 / 帧率 / 降载 / 停钟 / 沉夜 / 淤积
        all.add(new DepthChaosGlitch());
        all.add(new ColorWarpGlitch());
        all.add(new SignalLossGlitch());
        all.add(new FpsSpikeGlitch());
        all.add(new RenderDowngradeGlitch());
        all.add(new FrameLockGlitch());
        all.add(new GammaLockGlitch());
        all.add(new MemoryPressureGlitch());
        // HEAVY —— 矩阵 / 镜头 / 演出 / TPS / 鼠标
        all.add(new GeometryWarpGlitch());
        all.add(new CameraChaosGlitch());
        all.add(new FakeTdrGlitch());
        all.add(new TpsSpikeGlitch());
        all.add(new MouseFailureGlitch());
    }

    public List<RenderGlitch> all() {
        return Collections.unmodifiableList(all);
    }

    public boolean isBusy() {
        return current != null;
    }

    public String currentId() {
        return current == null ? null : current.id();
    }

    public void tick() {
        if (!CurseConfig.SPEC.isLoaded()) {
            return;
        }
        if (!CurseConfig.GLITCH_ENABLED.get()) {
            stopAll();
            return;
        }

        if (!cooldown.isEmpty()) {
            cooldown.replaceAll((k, v) -> v - 1);
            cooldown.values().removeIf(v -> v <= 0);
        }

        if (current != null) {
            current.onTick();
            if (--remaining <= 0) {
                endCurrent();
            }
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        RenderGlitch pick = pick();
        if (pick == null) {
            waitTicks = 40;
            return;
        }
        start(pick);
    }

    private RenderGlitch pick() {
        int maxLevel = CurseConfig.GLITCH_INTENSITY.get();
        List<RenderGlitch> pool = new ArrayList<>();
        for (RenderGlitch glitch : all) {
            if (glitch.severity().level() > maxLevel) {
                continue;
            }
            if (cooldown.containsKey(glitch.id())) {
                continue;
            }
            if (!allowedByConfig(glitch.id())) {
                continue;
            }
            pool.add(glitch);
        }
        if (pool.isEmpty()) {
            return null;
        }
        // 加权随机：weight 越大越常被抽中（fps_spike / tps_spike 权重高）
        int totalWeight = 0;
        for (RenderGlitch g : pool) {
            totalWeight += g.weight();
        }
        int roll = random.nextInt(totalWeight);
        int acc = 0;
        for (RenderGlitch g : pool) {
            acc += g.weight();
            if (roll < acc) {
                return g;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private static boolean allowedByConfig(String id) {
        return switch (id) {
            case MissingTextureGlitch.ID -> CurseConfig.G_MISSING_TEXTURE.get();
            case UvScrambleGlitch.ID -> CurseConfig.G_UV_SCRAMBLE.get();
            case ArtifactBlocksGlitch.ID -> CurseConfig.G_ARTIFACT_BLOCKS.get();
            case DepthChaosGlitch.ID -> CurseConfig.G_DEPTH_CHAOS.get();
            case ColorWarpGlitch.ID -> CurseConfig.G_COLOR_WARP.get();
            case SignalLossGlitch.ID -> CurseConfig.G_SIGNAL_LOSS.get();
            case FpsSpikeGlitch.ID -> CurseConfig.G_FPS_SPIKE.get();
            case RenderDowngradeGlitch.ID -> CurseConfig.G_RENDER_DOWNGRADE.get();
            case FrameLockGlitch.ID -> CurseConfig.G_FRAME_LOCK.get();
            case GammaLockGlitch.ID -> CurseConfig.G_GAMMA_LOCK.get();
            case MemoryPressureGlitch.ID -> CurseConfig.G_MEMORY_PRESSURE.get();
            case TpsSpikeGlitch.ID -> CurseConfig.G_TPS_SPIKE.get();
            case GeometryWarpGlitch.ID -> CurseConfig.G_GEOMETRY_WARP.get();
            case CameraChaosGlitch.ID -> CurseConfig.G_CAMERA_CHAOS.get();
            case FakeTdrGlitch.ID -> CurseConfig.G_FAKE_TDR.get();
            case MouseFailureGlitch.ID -> CurseConfig.G_MOUSE_FAILURE.get();
            default -> true;
        };
    }

    private void start(RenderGlitch glitch) {
        current = glitch;
        int min = CurseConfig.GLITCH_DURATION_MIN.get();
        int max = Math.max(min, CurseConfig.GLITCH_DURATION_MAX.get());
        int base = Mth.nextInt(random, min, max);
        // 个别故障（fps_spike / tps_spike）按 durationScale 拉长，上限 400 tick(20s)
        int scaled = Math.min(400, (int) (base * glitch.durationScale()));
        remaining = scaled;
        cooldown.put(glitch.id(), CurseConfig.GLITCH_COOLDOWN.get());
        // 每轮换种子 —— 贴图遗失的"坏哪一批"才会跟着换（否则每轮都是同样几张）
        GlitchRuntime.seed = random.nextInt();
        glitch.start(random);
    }

    private void endCurrent() {
        if (current != null) {
            current.stop();
            current = null;
        }
        remaining = 0;
        int min = CurseConfig.GLITCH_MIN_INTERVAL.get();
        int max = Math.max(min, CurseConfig.GLITCH_MAX_INTERVAL.get());
        waitTicks = Mth.nextInt(random, min, max);
    }

    /** 调试用：立刻播放指定故障（跳过等待和冷却，也不看强度过滤）。 */
    public boolean forceStart(String id) {
        for (RenderGlitch glitch : all) {
            if (glitch.id().equalsIgnoreCase(id)) {
                stopAll();
                start(glitch);
                return true;
            }
        }
        return false;
    }

    /** 停止一切并清空状态 —— panic / 关开关 / 关咒都走这里。 */
    public void stopAll() {
        if (current != null) {
            current.stop();
            current = null;
        }
        remaining = 0;
        waitTicks = 0;
        cooldown.clear();
        GlitchRuntime.resetAll();
    }
}
