package com.yuneko.sevencurses.client;

import com.yuneko.sevencurses.config.CurseConfig;
import com.yuneko.sevencurses.curse.CurseManager;
import com.yuneko.sevencurses.curse.CurseType;
import com.yuneko.sevencurses.glitch.GlitchRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 层：假花屏 / 黑幕 / 假 TDR 对话框 / 右下角状态提示。
 *
 * <p>这一层完全不碰 GL 状态，是整条「垂烬」里最安全的部分。
 *
 * <p>⚠️ 黑幕刻意做成「一次性盖住并保持」，<b>没有做高频闪烁</b> ——
 * 全屏黑白高频闪有光敏性癫痫风险，那不是整活是伤害。
 */
public final class CurseHudOverlay implements IGuiOverlay {

    public static final CurseHudOverlay INSTANCE = new CurseHudOverlay();

    /**
     * 假花屏用的贴图。
     *
     * <p>⚠️ 这是**占位贴图**（16x16）。自己画好之后直接替换这个文件即可，
     * 尺寸要求：任意 2 的幂正方形，然后把下面 TEX_SIZE 改成对应边长。
     */
    public static final ResourceLocation ARTIFACT_TEX =
            new ResourceLocation("sevencurses", "textures/glitch/artifact_noise.png");
    public static final int TEX_SIZE = 16;

    /** 故意选「显存坏了」会出的那种颜色。 */
    private static final int[] ARTIFACT_COLORS = {
            0xFF00FFFF, 0xFFFF00FF, 0xFF00FF00, 0xFFFFFF00,
            0xFF101010, 0xFFE01010, 0xFF1010E0, 0xFFF0F0F0
    };

    /** 假 TDR 对话框里随机挑一个驱动名。 */
    private static final String[] DRIVERS = {"nvlddmkm", "amdkmdag", "igdkmd64", "nvlddmkm"};

    private static volatile long lastClientTickMs = 0L;

    private CurseHudOverlay() {
    }

    /** 由 ClientBootstrap 每 tick 调一次，用来给花屏做「动画帧」。 */
    public static void markTick() {
        lastClientTickMs = System.currentTimeMillis();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!CurseConfig.SPEC.isLoaded() || !CurseConfig.GLOBAL_ENABLED.get()) {
            return;
        }

        // 1) 黑幕（断信 / 假死共用）
        if (GlitchRuntime.signalLoss) {
            int alpha = Math.max(0, Math.min(255, GlitchRuntime.signalLossAlpha));
            graphics.fill(0, 0, screenWidth, screenHeight, alpha << 24);
        }

        // 2) 假 TDR 对话框（浮在黑幕之上）
        if (GlitchRuntime.fakeTdr) {
            drawFakeTdr(graphics, screenWidth, screenHeight);
        }

        // 3) 假花屏
        if (GlitchRuntime.artifacts) {
            drawArtifacts(graphics, screenWidth, screenHeight);
        }

        // 4) 右下角状态
        if (CurseConfig.HUD_ENABLED.get()) {
            drawStatus(graphics, screenWidth, screenHeight);
        }
    }

    /* ------------------------------------------------------------------ */

    private void drawArtifacts(GuiGraphics g, int width, int height) {
        // 每 ~90ms 换一帧，做出「雪花跳动」的感觉（低频，不是高频闪烁）
        RandomSource rnd = RandomSource.create(GlitchRuntime.artifactSeed ^ (lastClientTickMs / 90L));
        int count = Math.max(0, Math.min(GlitchRuntime.artifactCount, 40));
        int maxX = Math.max(1, width);
        int maxY = Math.max(1, height);

        for (int i = 0; i < count; i++) {
            int x = rnd.nextInt(maxX);
            int y = rnd.nextInt(maxY);
            int bw = 6 + rnd.nextInt(Math.max(1, width / 4));
            int bh = 1 + rnd.nextInt(18);

            if (rnd.nextFloat() < 0.35f) {
                // 贴图版（占位贴图，替换后效果更真）
                g.blit(ARTIFACT_TEX, x, y, bw, bh, 0.0F, 0.0F,
                        TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
            } else {
                g.fill(x, y, Math.min(width, x + bw), Math.min(height, y + bh),
                        ARTIFACT_COLORS[rnd.nextInt(ARTIFACT_COLORS.length)]);
            }
        }

        // 整行撕裂，偶尔来一条
        if (rnd.nextFloat() < 0.4F) {
            int y = rnd.nextInt(maxY);
            int bh = 1 + rnd.nextInt(5);
            g.fill(0, y, width, Math.min(height, y + bh), 0x8000FFFF);
        }
    }

    /**
     * 仿 Windows「显示驱动程序已停止响应并且已恢复」对话框。
     *
     * <p>纯 HUD 演出：不弹真 Screen、不暂停游戏、不阻塞输入，几秒后自己消失。
     */
    private void drawFakeTdr(GuiGraphics g, int width, int height) {
        long elapsed = System.currentTimeMillis() - GlitchRuntime.fakeTdrStartMs;
        if (elapsed < 700L) {
            return; // 先纯黑 0.7 秒，再浮出对话框
        }

        int bw = Math.min(380, Math.max(240, width - 40));
        int bh = 132;
        int x = (width - bw) / 2;
        int y = (height - bh) / 2;

        // 投影 + 窗体
        g.fill(x + 5, y + 5, x + bw + 5, y + bh + 5, 0x90000000);
        g.fill(x, y, x + bw, y + bh, 0xFFF0F0F0);
        // 标题栏
        g.fill(x, y, x + bw, y + 26, 0xFF1B3D6B);
        // 内容区
        g.fill(x + 1, y + 27, x + bw - 1, y + bh - 1, 0xFFE8E8E8);

        Font font = Minecraft.getInstance().font;
        g.drawString(font, "⚠  显示驱动程序停止响应", x + 8, y + 9, 0xFFFFFFFF, false);

        int seed = GlitchRuntime.seed;
        String driver = DRIVERS[Math.floorMod(seed, DRIVERS.length)];
        int tdrCount = 1 + Math.floorMod(seed >>> 8, 12);

        g.drawString(font, "显示驱动程序 " + driver + " 已停止响应并已恢复。",
                x + 14, y + 44, 0xFF101010, false);
        g.drawString(font, "TDR 计数：" + tdrCount + "    已尝试复位 GPU。",
                x + 14, y + 62, 0xFF404040, false);
        g.drawString(font, "画面可能短暂异常。",
                x + 14, y + 78, 0xFF404040, false);

        // 假按钮
        int btnW = 76;
        int btnH = 22;
        int bx = x + bw - btnW - 14;
        int by = y + bh - btnH - 14;
        g.fill(bx, by, bx + btnW, by + btnH, 0xFFDCDCDC);
        g.fill(bx, by, bx + btnW, by + 1, 0xFFFFFFFF);
        g.fill(bx, by + btnH - 1, bx + btnW, by + btnH, 0xFF9A9A9A);
        g.drawString(font, "确定", bx + 25, by + 7, 0xFF101010, false);
    }

    private void drawStatus(GuiGraphics g, int width, int height) {
        List<Component> lines = new ArrayList<>();

        List<String> names = new ArrayList<>();
        for (CurseType type : CurseType.values()) {
            if (CurseManager.get().isEnabled(type)) {
                names.add(Component.translatable(type.translationKey()).getString());
            }
        }
        if (!names.isEmpty()) {
            lines.add(Component.translatable("sevencurses.hud.active", String.join(" / ", names)));
        }

        String glitch = CurseManager.get().glitches().currentId();
        if (glitch != null) {
            lines.add(Component.translatable("sevencurses.hud.glitch",
                    Component.translatable("glitch.sevencurses." + glitch).getString()));
        }

        if (lines.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int y = height - 4 - lines.size() * (font.lineHeight + 1);
        for (Component line : lines) {
            int w = font.width(line);
            g.drawString(font, line.getVisualOrderText(), width - 4 - w, y, 0xFFB060FF, true);
            y += font.lineHeight + 1;
        }
    }
}
