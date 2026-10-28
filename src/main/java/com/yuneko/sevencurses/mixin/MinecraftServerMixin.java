package com.yuneko.sevencurses.mixin;

import com.yuneko.sevencurses.ServerGlitchState;
import com.yuneko.sevencurses.config.CurseConfig;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * 「迟世」（TPS 抽风）—— 直接 Mixin {@link MinecraftServer#tickServer}。
 *
 * <p>之前用 {@code ServerTickEvent} 监听不靠谱（事件总线可能没触发），
 * 改成直接在 tickServer 入口睡 → mspt 飙到 60~250 → TPS 掉到 4~8，
 * 怪物冻结、方块更新延迟。
 *
 * <p><b>v1.4.0 加了两件事，专门治"看起来没生效"：</b>
 * <ol>
 *   <li>每 tick 无条件 {@code tickSeen++}。这个计数只要在涨，就说明 Mixin
 *       确实挂在 tickServer 上 —— 把"没生效"和"效果不明显"彻底分开。</li>
 *   <li>在 RETURN 处记 nanoTime，算出<b>实测 tick 周期</b>（含我们自己的睡眠），
 *       喂给 {@link ServerGlitchState#avgPeriodMs}，供 {@code /curse diag} 与 F3 显示真值。</li>
 * </ol>
 *
 * <p>在 client mixin 列表里 → 单机集成服务器（同进程）能读到。
 * 独服没跑我们的调度器 → flag 不被 set → 不生效。
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    private static final Random RND = new Random();

    @Inject(method = "tickServer(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void sevencurses$tpsSpike(BooleanSupplier supplier, CallbackInfo ci) {
        // ① 自证心跳：Mixin 活着，这个数就一直在涨
        ServerGlitchState.tickSeen++;

        // ② 实测 tick 周期（上一轮 RETURN → 这一轮 HEAD）
        long now = System.nanoTime();
        long prev = ServerGlitchState.lastReturnNs;
        if (prev != 0L) {
            double period = (now - prev) / 1_000_000.0;
            // 超过 2s 的样本是暂停 / 载入 / 切换维度，丢掉，别污染 TPS 读数
            if (period > 0.5 && period < 2000.0) {
                ServerGlitchState.lastPeriodMs = period;
                double ema = ServerGlitchState.avgPeriodMs;
                ServerGlitchState.avgPeriodMs = ema <= 0.0 ? period : ema * 0.75 + period * 0.25;
            }
        }

        if (!ServerGlitchState.tpsSpike) {
            return;
        }

        // ③ 真睡：60~200ms/tick → 20 TPS 掉到 4~8
        int min = 60;
        int max = 200;
        try {
            if (CurseConfig.SPEC.isLoaded()) {
                min = CurseConfig.TPS_SLEEP_MIN.get();
                max = Math.max(min, CurseConfig.TPS_SLEEP_MAX.get());
            }
        } catch (Throwable ignored) {
            // config 没加载好就用默认值
        }
        int ms = min + RND.nextInt(Math.max(1, max - min + 1));
        try {
            Thread.sleep(ms);
            ServerGlitchState.recordSleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Inject(method = "tickServer(Ljava/util/function/BooleanSupplier;)V", at = @At("RETURN"))
    private void sevencurses$markReturn(BooleanSupplier supplier, CallbackInfo ci) {
        ServerGlitchState.lastReturnNs = System.nanoTime();
    }
}
