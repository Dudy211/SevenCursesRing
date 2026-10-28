package com.yuneko.sevencurses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yuneko.sevencurses.ServerGlitchState;
import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.curse.CurseManager;
import com.yuneko.sevencurses.curse.CurseType;
import com.yuneko.sevencurses.glitch.RenderGlitch;
import com.yuneko.sevencurses.glitch.impl.MemoryPressureGlitch;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 客户端指令 {@code /curse}。
 *
 * <pre>
 *   /curse list                     列出七咒状态（带真实作用）
 *   /curse toggle &lt;id&gt;              开/关某一咒
 *   /curse glitch list              列出 16 个渲染故障
 *   /curse glitch now &lt;id&gt;          立刻播放某个故障（跳过随机与强度过滤）
 *   /curse glitch stop              立刻停止当前故障
 *   /curse diag                     诊断：Mixin 存活 / 实测 mspt / 淤积占用
 *   /curse panic                    一键平息
 * </pre>
 *
 * <p>注册到**客户端** dispatcher，所以进任何服务器都能用，且不会影响服务端。
 */
public final class CurseCommand {

    private CurseCommand() {
    }

    private static final SuggestionProvider<CommandSourceStack> CURSE_IDS = (ctx, builder) -> {
        for (CurseType type : CurseType.values()) {
            builder.suggest(type.id());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> GLITCH_IDS = (ctx, builder) -> {
        for (RenderGlitch glitch : CurseManager.get().glitches().all()) {
            builder.suggest(glitch.id());
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("curse")
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))

                .then(Commands.literal("toggle")
                        .then(Commands.argument("curse", StringArgumentType.word())
                                .suggests(CURSE_IDS)
                                .executes(ctx -> toggle(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "curse")))))

                .then(Commands.literal("glitch")
                        .then(Commands.literal("list")
                                .executes(ctx -> glitchList(ctx.getSource())))
                        .then(Commands.literal("now")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(GLITCH_IDS)
                                        .executes(ctx -> glitchNow(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("stop")
                                .executes(ctx -> {
                                    CurseManager.get().glitches().stopAll();
                                    return feedback(ctx.getSource(), "§a已停止当前渲染故障，状态已复原。");
                                })))

                .then(Commands.literal("diag")
                        .executes(ctx -> diag(ctx.getSource())))

                .then(Commands.literal("panic")
                        .executes(ctx -> {
                            CurseManager.get().panic();
                            return feedback(ctx.getSource(), "§a§l七咒已平息（渲染 / 按键 / 内存状态全部复原）。");
                        })));
    }

    /* ------------------------------------------------------------------ */

    private static int list(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder("§6七咒之戒 §7—— 状态\n");
        for (CurseType type : CurseType.values()) {
            boolean on = CurseManager.get().isEnabled(type);
            String mark = on ? "§a[开]" : "§8[关]";
            String tag = type.implemented() ? "" : " §8(未实现)";
            sb.append("  ").append(mark).append(" §f").append(type.id())
                    .append(" §b").append(Component.translatable(type.translationKey()).getString())
                    .append(" §8· §7").append(Component.translatable(type.translationKey() + ".effect").getString())
                    .append(tag).append('\n');
        }
        sb.append("§7用 §f/curse toggle <id>§7 切换；§f/curse panic§7 一键解除；§f/curse diag§7 诊断。");
        return feedback(source, sb.toString());
    }

    private static int toggle(CommandSourceStack source, String rawId) {
        var type = CurseType.byId(rawId);
        if (type.isEmpty()) {
            return feedback(source, "§c没有这个咒：" + rawId);
        }
        boolean nowOn = CurseManager.get().toggle(type.get());
        String name = Component.translatable(type.get().translationKey()).getString();
        if (nowOn && !type.get().implemented()) {
            return feedback(source, "§e" + name + " §7已开启，但这一咒还没实现，暂时不会有任何效果。");
        }
        return feedback(source, (nowOn ? "§a已开启 " : "§c已关闭 ") + "§f" + name);
    }

    private static int glitchList(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder("§6垂烬 §7—— 可用故障\n");
        String current = CurseManager.get().glitches().currentId();
        for (RenderGlitch glitch : CurseManager.get().glitches().all()) {
            boolean active = glitch.id().equals(current);
            sb.append("  ").append(active ? "§a▶ " : "§8  ")
                    .append("§f").append(glitch.id())
                    .append(" §7").append(glitch.severity().name().toLowerCase(java.util.Locale.ROOT))
                    .append(" §8").append(Component.translatable("glitch.sevencurses." + glitch.id()).getString())
                    .append('\n');
        }
        sb.append("§7用 §f/curse glitch now <id>§7 立刻播放。");
        return feedback(source, sb.toString());
    }

    private static int glitchNow(CommandSourceStack source, String id) {
        boolean ok = CurseManager.get().glitches().forceStart(id);
        if (!ok) {
            return feedback(source, "§c没有这个故障：" + id + "（试试 /curse glitch list）");
        }
        return feedback(source, "§a已播放渲染故障：§f" + id);
    }

    /**
     * 诊断面板。存在的唯一目的：把「没生效」和「效果不明显」分开。
     *
     * <p>{@code tickServer 调用} 不涨 = Mixin 没挂上；
     * 涨了但 {@code 睡掉 0 tick} = flag 没被置起来（咒没开 / 被强度过滤）；
     * 睡了却 {@code TPS 仍然 20} = 别的问题（这时候再看实测 mspt）。
     */
    private static int diag(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder("§6七咒之戒 §7—— 诊断\n");

        // 1) 迟世（TPS）
        double period = ServerGlitchState.avgPeriodMs;
        sb.append("§7迟世：").append(ServerGlitchState.tpsSpike ? "§a在放" : "§8未放")
                .append("§7  睡掉 §f").append(ServerGlitchState.sleptTicks).append(" §7tick / §f")
                .append(ServerGlitchState.sleptMs).append("§7ms")
                .append("  兜底=").append(ServerGlitchState.eventFallback ? "§e开" : "§8关")
                .append('\n');
        sb.append("§7tickServer 调用 §f").append(ServerGlitchState.tickSeen)
                .append(" §7次").append(ServerGlitchState.tickSeen > 0 ? "§a（Mixin 存活）" : "§c（Mixin 没跑过）")
                .append('\n');
        if (period > 0.0) {
            sb.append(String.format(java.util.Locale.ROOT,
                    "§7实测 mspt §f%.1f§7  推得 TPS §f%.1f§7\n", period, ServerGlitchState.tps()));
        } else {
            sb.append("§7实测 mspt：§8还没有样本（服务端还没 tick 过）\n");
        }

        // 2) 淤积（内存）
        long maxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        sb.append("§7淤积：").append(memFlag())
                .append('\n');
        sb.append("§7  ").append(MemoryPressureGlitch.stats()).append('\n');
        sb.append("§7堆：§f").append(usedMb).append("§7 / §f").append(maxMb).append("§7MB\n");

        // 3) 配置口径
        if (CurseConfig.SPEC.isLoaded()) {
            sb.append("§7maxIntensity=§f").append(CurseConfig.GLITCH_INTENSITY.get())
                    .append("§7  tps_spike=").append(onOff(CurseConfig.G_TPS_SPIKE.get()))
                    .append("§7  memory=").append(onOff(CurseConfig.G_MEMORY_PRESSURE.get()))
                    .append("§7  hold=§f").append(CurseConfig.MEM_HOLD_FRACTION.get())
                    .append("§7  churn=").append(onOff(CurseConfig.MEM_CHURN.get()))
                    .append('\n');
            sb.append("§7睡眠区间：§f").append(CurseConfig.TPS_SLEEP_MIN.get())
                    .append("§7~§f").append(CurseConfig.TPS_SLEEP_MAX.get()).append("§7ms/tick\n");
        }
        sb.append("§8提示：/curse glitch now tps_spike 或 now memory_pressure 可跳过随机，直接放一次。");
        return feedback(source, sb.toString());
    }

    private static String memFlag() {
        return com.yuneko.sevencurses.glitch.GlitchRuntime.memoryPressure ? "§a在放" : "§8未放";
    }

    private static String onOff(boolean b) {
        return b ? "§a开" : "§c关";
    }

    private static int feedback(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
