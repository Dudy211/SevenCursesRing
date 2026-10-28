package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 空间拉伤：整个世界被拉伸 / 压扁 / 漂移。
 *
 * <p>实现方式：在 {@link com.yuneko.sevencurses.mixin.LevelRendererMixin} 的
 * {@code LevelRenderer.renderLevel} HEAD 里直接改那个 PoseStack。
 *
 * <p>✅ 这样是安全的，理由是字节码实证过的：
 * <pre>
 *   GameRenderer.render(...)  偏移 233:  new PoseStack()      ← 每帧新建
 *                             偏移 240:  renderLevel(FJLPoseStack;)V
 * </pre>
 * 也就是说传进 {@code renderLevel} 的 PoseStack 是<b>每帧一个全新的局部对象</b>，
 * 改它不会跳帧累积，也<b>不需要 push/pop</b> —— 因此不存在
 * 「push/pop 不配对 → Pose stack not empty → 进世界就崩」那个经典坑。
 *
 * <p>变形量用慢速正弦驱动（不是逐帧随机），否则会变成高频抖动，既不像显卡故障又晃眼。
 */
public final class GeometryWarpGlitch implements RenderGlitch {

    public static final String ID = "geometry_warp";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.HEAVY;
    }

    @Override
    public void start(RandomSource random) {
        GlitchRuntime.seed = random.nextInt();
        GlitchRuntime.geometryAmount = 0.06f + random.nextFloat() * 0.14f;
        GlitchRuntime.geometryWarp = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.geometryWarp = false;
    }
}
