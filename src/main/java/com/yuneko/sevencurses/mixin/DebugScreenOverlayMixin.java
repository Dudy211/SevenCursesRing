package com.yuneko.sevencurses.mixin;

import com.yuneko.sevencurses.ServerGlitchState;
import com.yuneko.sevencurses.curse.CurseManager;
import com.yuneko.sevencurses.curse.CurseType;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import com.yuneko.sevencurses.glitch.impl.MemoryPressureGlitch;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * F3 伪装 + F3 验货。
 *
 * <p>原版 F3 系统信息只有 Java / Mem / Allocated / Allocation rate / CPU / Display
 * —— <b>根本没有 GPU 行</b>（已字节码实证）。所以这里加几行是纯增量，不会撞车：
 * <ul>
 *   <li>{@code GPU: <型号>  [0% 已挂起, TDR #N]} —— 配合 visible 的渲染故障</li>
 *   <li>{@code mspt: XX.X  TPS: N} —— 没在放「迟世」时是瞎编的；<b>正在放「迟世」时显示的是
 *       实测值</b>（{@link ServerGlitchState#avgPeriodMs}），因为这时候你要看的是真数。</li>
 *   <li>{@code 淤积: held / target} —— 「锁内存」正在放时，直接告诉你按住多少 MB。</li>
 * </ul>
 *
 * <p>只在<b>有故障正在播</b>时塞（{@code isBusy()}），间歇性故障 → 间歇性假数据，
 * 这才是最像真的。没在播的时候 F3 干干净净，符合 vanilla。
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    private static final String[] FAKE_DRIVERS = {
            "NVIDIA GeForce RTX 3080",
            "AMD Radeon RX 6800",
            "Intel UHD Graphics 770"
    };

    @Inject(method = "getSystemInformation()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void sevencurses$fakeF3(CallbackInfoReturnable<List<String>> cir) {
        if (!CurseManager.get().isEnabled(CurseType.GPU)) {
            return;
        }
        if (!CurseManager.get().globalEnabled()) {
            return;
        }
        // 只在有故障正在播时塞 —— 没在播的时候 F3 保持 vanilla 干净
        if (!CurseManager.get().glitches().isBusy()) {
            return;
        }

        List<String> original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        List<String> lines = new ArrayList<>(original);

        int seed = GlitchRuntime.seed;
        long t = System.currentTimeMillis();
        String driver = FAKE_DRIVERS[Math.floorMod(seed, FAKE_DRIVERS.length)];

        lines.add("GPU: " + driver + "  [0% 已挂起, TDR #" + (1 + Math.floorMod(seed >>> 6, 12)) + "]");

        // mspt / TPS：迟世在放就用实测值，其余时候维持原有的"瞎编但看起来合理"
        double mspt;
        double tps;
        String suffix = "  (display driver recovered)";
        if (ServerGlitchState.tpsSpike) {
            double real = ServerGlitchState.avgPeriodMs;
            mspt = real > 0.0 ? real : ServerGlitchState.lastPeriodMs;
            if (mspt <= 0.0) {
                mspt = 50.0;
            }
            tps = Math.min(20.0, 1000.0 / mspt);
            suffix = "  [七咒 · 迟世 / 实测]";
        } else {
            mspt = 60.0 + Math.abs(Math.sin(t / 90.0)) * 90.0;
            tps = Math.min(20.0, 1000.0 / Math.max(1.0, mspt));
        }
        lines.add(String.format(java.util.Locale.ROOT,
                "mspt: %.1f  TPS: %.1f%s", mspt, tps, suffix));

        // 淤积在放就把真实占用量摆出来
        if (GlitchRuntime.memoryPressure) {
            lines.add(String.format(java.util.Locale.ROOT,
                    "淤积: held %dMB / target %dMB  churn %dMB",
                    MemoryPressureGlitch.heldBytes / (1024 * 1024),
                    MemoryPressureGlitch.targetBytes / (1024 * 1024),
                    MemoryPressureGlitch.churnMb));
        }

        cir.setReturnValue(lines);
    }
}
