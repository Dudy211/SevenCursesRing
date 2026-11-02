package com.yuneko.sevencurses.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yuneko.sevencurses.ServerGlitchState;
import com.yuneko.sevencurses.SevenCursesMod;
import com.yuneko.sevencurses.command.CurseCommand;
import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.curse.CurseManager;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.boon.BoonService;
import com.yuneko.sevencurses.glitch.impl.FrameLockGlitch;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端接线。
 *
 * <p>只用 {@code IEventBus.addListener(...)} 显式绑定，不用 {@code @EventBusSubscriber} ——
 * 少一层注解扫描，行为更可预期。
 *
 * <p>MOD bus：注册按键 / GUI overlay（都发生在 mod 构造期）。
 * <p>FORGE bus：tick / 客户端指令 / 镜头角度。
 */
public final class ClientBootstrap {

    /** 紧急解除键：默认 DELETE。 */
    public static final KeyMapping PANIC_KEY = new KeyMapping(
            "key.sevencurses.panic",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DELETE,
            "key.categories.sevencurses");

    private ClientBootstrap() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::onRegisterKeyMappings);
        modBus.addListener(ClientBootstrap::onRegisterOverlays);

        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onRegisterClientCommands);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onComputeCameraAngles);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onComputeFogColor);
        // 七福「净」要用实测帧率 —— 每帧喂一次（只在测帧期有效，测完自动停）
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::onRenderStage);
        // 七咒之戒的自绘 tooltip（原版 tooltip 是 List<Component>，没法让字转）
        MinecraftForge.EVENT_BUS.addListener(RingTooltipRenderer::onPre);

        // 把"七咒之戒"的 curioTick 回调接到 CurseManager
        ClientRingHooks.install();

        SevenCursesMod.LOG.info("[SevenCurses] 客户端侧接线完成（MOD bus + FORGE bus）。");
    }

    /* ---- MOD bus ---- */

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PANIC_KEY);
    }

    private static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("sevencurses_hud", CurseHudOverlay.INSTANCE);
    }

    /* ---- FORGE bus ---- */

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        // 客户端指令：单机和别人的服务器上都能用（它只存在于客户端 dispatcher）。
        CurseCommand.register(event.getDispatcher());
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // 紧急解除：点一下 DELETE 立刻清空全部诅咒
        while (PANIC_KEY.consumeClick()) {
            CurseManager.get().panic();
        }
        CurseManager.get().clientTick();
        // 硬件探针（只探一次）+ 七福「缓」的帧率上限维护
        HardwareProbe.tick();
        tickFrameCap();
        // 第 3 咒 · 帧率抽风：在主线程上随机睡一下，制造 FPS 突然跳动 / 偶尔卡一帧。
        // 只动时间、不动 GL / 内存 / 存档，最坏就是一次几百毫秒的卡顿，下帧自动恢复。
        if (GlitchRuntime.fpsSpike) {
            spikeFrame();
        }
        CurseHudOverlay.markTick();
    }

    private static final java.util.Random FPS_RND = new java.util.Random();

    /* ---- 七福 · 缓 ---- */

    /** 玩家原本的帧率设置；-1 = 当前没在托管。 */
    private static int savedFrameCap = -1;

    /**
     * 「缓」：戴着戒指时把帧率上限按显卡档位反向压住，摘下来还原玩家自己的设置。
     *
     * <p>⚠️ 停钟（frame_lock）正在播时让路 —— 否则常驻上限会把 5~15fps 的暴击瞬间覆盖掉。
     * <p>⚠️ framerateLimit 是持久化选项（会写进 options.txt），所以摘下 / 退游时必须还原。
     */
    private static void tickFrameCap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return;
        }
        // 测帧期间一律不压帧率：要量的是这台机器的真实表现，不是被我们掐过的 10fps
        if (!com.yuneko.sevencurses.boon.HardwareProfile.fpsMeasured) {
            return;
        }
        if (FrameLockGlitch.active) {
            return;   // 这一轮让给「停钟」
        }
        if (!CurseManager.get().ringEquipped()) {
            if (savedFrameCap >= 0) {
                mc.options.framerateLimit().set(savedFrameCap);
                savedFrameCap = -1;
            }
            return;
        }
        int level = mc.player == null ? 0 : mc.player.experienceLevel;
        int cap = BoonService.frameCap(level);
        if (savedFrameCap < 0) {
            savedFrameCap = mc.options.framerateLimit().get();
        }
        if (mc.options.framerateLimit().get() != cap) {
            mc.options.framerateLimit().set(cap);
        }
    }

    /**
     * 第 7 咒 · 迟世的<b>兜底通道</b>。
     *
     * <p>平时睡眠发生在 {@code MinecraftServer.tickServer} 的 Mixin 里。
     * 万一 Mixin 没挂上（换游戏版本 / 别的模组抢了注入点 / 映射漂移），
     * {@code TpsSpikeGlitch.onTick()} 会发现 {@code tickSeen} 十个 tick 不动，
     * 把 {@link ServerGlitchState#eventFallback} 打开 —— 然后这里接手。
     *
     * <p>它跑在服务端线程（{@code ServerTickEvent} 的调用线程），所以睡下去
     * 一样是睡在世界的心跳上，效果与 Mixin 路径完全一致。
     */
    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ServerGlitchState.eventFallback || !ServerGlitchState.tpsSpike) {
            return;
        }
        int min = 60;
        int max = 200;
        try {
            if (CurseConfig.SPEC.isLoaded()) {
                min = CurseConfig.TPS_SLEEP_MIN.get();
                max = Math.max(min, CurseConfig.TPS_SLEEP_MAX.get());
            }
        } catch (Throwable ignored) {
            // 用默认值
        }
        int ms = min + FPS_RND.nextInt(Math.max(1, max - min + 1));
        try {
            Thread.sleep(ms);
            ServerGlitchState.recordSleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void spikeFrame() {
        // 「乱跳」而不是「一直低」：4 成概率不睡（FPS 正常），其余阶梯式掉
        float roll = FPS_RND.nextFloat();
        try {
            if (roll < 0.40f) {
                // 不睡 —— FPS 正常，制造跳来跳去的感觉
            } else if (roll < 0.75f) {
                Thread.sleep(10 + FPS_RND.nextInt(30));    // 10~40ms 轻微跳
            } else if (roll < 0.90f) {
                Thread.sleep(50 + FPS_RND.nextInt(70));    // 50~120ms 明显掉
            } else {
                Thread.sleep(150 + FPS_RND.nextInt(150)); // 150~300ms 卡一帧
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 第 3 咒 · 镜头抽搐。
     *
     * <p>用慢速正弦而不是逐帧随机 —— 逐帧随机等于画面高频抽搐，既不像显卡故障，
     * 也很容易把人晃晕。
     */
    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!GlitchRuntime.cameraChaos) {
            return;
        }
        event.setRoll(event.getRoll() + GlitchRuntime.osc(1.7F, 0.0F) * GlitchRuntime.cameraRoll);
        event.setYaw(event.getYaw() + GlitchRuntime.osc(2.3F, 1.1F) * GlitchRuntime.cameraYaw);
        event.setPitch(event.getPitch() + GlitchRuntime.osc(3.1F, 2.2F) * GlitchRuntime.cameraPitch);
    }

    /** 每帧一次：把这一帧喂给硬件探针（七福「净」的实测帧率）。 */
    private static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            HardwareProbe.onFrame();
        }
    }

    /** 「沉夜」：把雾色压到接近黑，配合 gamma=0 让世界几乎全黑。 */
    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (GlitchRuntime.gammaDark) {
            event.setRed(0.05F);
            event.setGreen(0.05F);
            event.setBlue(0.10F);
        }
    }
}
