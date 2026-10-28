package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.ParticleStatus;
import net.minecraft.util.RandomSource;

/**
 * 褪色：把渲染设置打到地板 → GPU 占用真的掉下去。
 *
 * <p>改的是「瞬时生效、不触发资源重载」的选项：
 * <ul>
 *   <li>渲染距离 → 2（会触发 allChanged 异步重排区块，不卡死，但区块会缩到最小）</li>
 *   <li>云 → 关</li>
 *   <li>实体阴影 → 关</li>
 *   <li>环境光遮蔽 → 关</li>
 *   <li>生物群系过渡 → 0</li>
 *   <li>粒子 → 最少</li>
 * </ul>
 *
 * <p>⚠️ 刻意<b>不碰</b> graphicsMode 和 mipmapLevels —— 这俩会触发完整资源重载，
 * 卡好几秒，跟一个 2~6 秒的故障对不上。
 *
 * <p>{@link #stop()} 把所有值还原回故障前的原始值。
 */
public final class RenderDowngradeGlitch implements RenderGlitch {

    public static final String ID = "render_downgrade";

    private static boolean saved = false;
    private static int origRenderDistance;
    private static CloudStatus origClouds;
    private static boolean origShadows;
    private static boolean origAO;
    private static int origBiomeBlend;
    private static ParticleStatus origParticles;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public GlitchSeverity severity() {
        return GlitchSeverity.MEDIUM;
    }

    @Override
    public void start(RandomSource random) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.level == null) {
            return;
        }
        OptionInstance<Integer> rd = mc.options.renderDistance();
        OptionInstance<CloudStatus> clouds = mc.options.cloudStatus();
        OptionInstance<Boolean> shadows = mc.options.entityShadows();
        OptionInstance<Boolean> ao = mc.options.ambientOcclusion();
        OptionInstance<Integer> blend = mc.options.biomeBlendRadius();
        OptionInstance<ParticleStatus> parts = mc.options.particles();

        origRenderDistance = rd.get();
        origClouds = clouds.get();
        origShadows = shadows.get();
        origAO = ao.get();
        origBiomeBlend = blend.get();
        origParticles = parts.get();

        rd.set(2);
        clouds.set(CloudStatus.OFF);
        shadows.set(false);
        ao.set(false);
        blend.set(0);
        parts.set(ParticleStatus.MINIMAL);

        saved = true;
    }

    @Override
    public void stop() {
        if (!saved) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            saved = false;
            return;
        }
        mc.options.renderDistance().set(origRenderDistance);
        mc.options.cloudStatus().set(origClouds);
        mc.options.entityShadows().set(origShadows);
        mc.options.ambientOcclusion().set(origAO);
        mc.options.biomeBlendRadius().set(origBiomeBlend);
        mc.options.particles().set(origParticles);
        saved = false;
    }
}
