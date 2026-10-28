package com.yuneko.sevencurses.glitch.impl;

import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.GlitchSeverity;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import net.minecraft.util.RandomSource;

/**
 * 目眩：画面自己转起来（roll 倾斜 + yaw/pitch 摇摆）。
 *
 * <p>实现方式：Forge 的 <b>{@code ViewportEvent.ComputeCameraAngles}</b> 事件
 * （已 javap 确认有 getRoll/setRoll/getYaw/setYaw/getPitch/setPitch）。
 *
 * <p>这是整条 HEAVY 组里最安全的一种 —— 镜头角度每帧都会被原版重算，
 * 我们只是往结果上叠一个偏移，没有任何状态需要还原。
 *
 * <p>用慢速正弦而不是逐帧随机：逐帧随机 = 画面高频抽搽，容易把人晃晕。
 */
public final class CameraChaosGlitch implements RenderGlitch {

    public static final String ID = "camera_chaos";

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
        GlitchRuntime.cameraRoll = 12.0f + random.nextFloat() * 22.0f;
        GlitchRuntime.cameraYaw = 4.0f + random.nextFloat() * 9.0f;
        GlitchRuntime.cameraPitch = 3.0f + random.nextFloat() * 7.0f;
        GlitchRuntime.cameraChaos = true;
    }

    @Override
    public void stop() {
        GlitchRuntime.cameraChaos = false;
    }
}
