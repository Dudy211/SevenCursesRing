package com.yuneko.sevencurses.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yuneko.sevencurses.boon.BoonType;
import com.yuneko.sevencurses.curse.CurseType;
import com.yuneko.sevencurses.item.SevenCursesItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 七咒之戒的<b>自绘 tooltip</b>。
 *
 * <p>原版 tooltip 只是一个 {@code List<Component>}，不可能让字转起来。所以这里在
 * {@link RenderTooltipEvent.Pre} 里直接 {@code cancel} 掉原版，自己画面板：
 *
 * <pre>
 *   悬浮       → 14 个单字：红=七咒、蓝=七福，分两层同步呼吸
 *   按住 Shift → 七咒详情
 *   按住 Alt   → 七福详情
 * </pre>
 *
 * <h3>动效模型（两个变量，仅此而已）</h3>
 * <pre>
 *   φ   = t / CYCLE × 2π                     // 呼吸相位
 *   p咒 = (1 − cos φ) / 2                     // 0=最内 1=最外
 *   p福 = (1 + cos φ) / 2                     // 与咒严格反相
 *   r   = rIn + (rOut − rIn) × p
 *   角度 = angBase + k×51.43° (+ 25.71° 给福)  // 两组固定错开半格
 *   angBase += SPIN × dt                      // 整束一起自转
 * </pre>
 *
 * <p><b>为什么不会叠字：</b>两组的角差是死值 25.71°，不随转动漂移。所以内层胀出来、
 * 外层收进去、半径相等的那一刻，红蓝正好相差半格 —— 从对方的空位里穿过去。
 * 自转多快都不影响（实测 0~45°/s 时最近距离恒为 69px，字宽 19px）。
 */
public final class RingTooltipRenderer {

    /* ---- 尺寸 / 节奏（对齐 demo 的定稿参数） ---- */
    private static final int SIZE = 168;
    private static final double GAP = 360.0D / 7.0D;   // 同组相邻字 51.43°
    private static final double HALF_GAP = GAP * 0.5D; // 两组错开 25.71°
    private static final double CYCLE = 18.0D;         // 一次呼吸 18 秒
    private static final double SPIN = 30.0D;          // 整束自转 30°/s

    /* ---- 配色 ---- */
    private static final int C_PANEL = 0xF01C0A2C;
    private static final int C_BORDER = 0xFF5B2D8E;
    private static final int C_BORDER_SOFT = 0x40B48CFF;
    private static final int C_TITLE = 0xFFF3E9FF;
    private static final int C_HINT = 0xFF7E6D97;
    private static final int C_CURSE = 0xFFFF5A5A;
    private static final int C_BOON = 0xFF4FE3FF;
    private static final int C_TEXT = 0xFFA99BC0;

    /* ---- 动画状态 ---- */
    private static double angBase = 0.0D;
    private static double tSec = 0.0D;
    private static double lastSec = 0.0D;

    private RingTooltipRenderer() {
    }

    /* ================================================================== */

    public static void onPre(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();
        if (stack == null || !stack.is(SevenCursesItems.SEVEN_CURSES_RING.get())) {
            return;
        }
        // 接管：原版 tooltip 不画了，剩下全归我们
        event.setCanceled(true);

        GuiGraphics g = event.getGraphics();
        Font font = event.getFont();
        if (g == null || font == null) {
            return;
        }

        boolean curseDetail = Screen.hasShiftDown();
        boolean boonDetail = !curseDetail && altDown();

        double nowSec = System.nanoTime() / 1.0E9D;
        double dt = lastSec == 0 ? 0 : Math.min(0.05D, Math.max(0, nowSec - lastSec));
        lastSec = nowSec;
        tSec += dt;
        angBase = (angBase + SPIN * dt) % 360.0D;

        // ---- 面板尺寸 ----
        List<String> rows = null;
        int rowColor = C_TEXT;
        if (curseDetail) {
            rows = curseRows();
            rowColor = C_CURSE;
        } else if (boonDetail) {
            rows = boonRows();
            rowColor = C_BOON;
        }

        int panelW = SIZE;
        int panelH = SIZE;
        if (rows != null) {
            int maxW = 0;
            for (String s : rows) {
                maxW = Math.max(maxW, font.width(s));
            }
            panelW = Math.max(SIZE, maxW + 22);
            panelH = 26 + rows.size() * 10 + 8;
        }

        int px = clamp(event.getX() + 12, 4, Math.max(4, event.getScreenWidth() - panelW - 4));
        int py = clamp(event.getY() - 12, 4, Math.max(4, event.getScreenHeight() - panelH - 4));

        // ★ 原版 tooltip 是 translate(0,0,400) 画的；物品图标在 z=150。
        //   直接在 Pre 里画的话 z=0，会被物品盖住 —— 所以这里必须自己抬到 400。
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, 400.0F);

        // ---- 背景 ----
        // 外发光（三层，越外越淡）
        for (int i = 3; i >= 1; i--) {
            int a = 0x0B * (4 - i);
            g.fill(px - i - 1, py - i - 1, px + panelW + i + 1, py + panelH + i + 1, (a << 24) | 0x7B3FD0);
        }
        // 边框
        g.fill(px - 1, py - 1, px + panelW + 1, py + panelH + 1, C_BORDER);
        // 竖向渐变（8 段拼出来）
        for (int i = 0; i < 8; i++) {
            int y0 = py + panelH * i / 8;
            int y1 = py + panelH * (i + 1) / 8;
            g.fill(px, y0, px + panelW, y1, colorMix(0x2A1240, 0x0E0618, i / 7.0D));
        }
        // 内侧一圈柔光
        g.fill(px + 1, py + 1, px + panelW - 1, py + 2, C_BORDER_SOFT);
        // 四角短线装饰
        int cLen = 12, cCol = 0xA0C9A6FF;
        g.fill(px + 1, py + 1, px + 1 + cLen, py + 2, cCol);
        g.fill(px + 1, py + 1, px + 2, py + 1 + cLen, cCol);
        g.fill(px + panelW - 1 - cLen, py + 1, px + panelW - 1, py + 2, cCol);
        g.fill(px + panelW - 2, py + 1, px + panelW - 1, py + 1 + cLen, cCol);
        g.fill(px + 1, py + panelH - 2, px + 1 + cLen, py + panelH - 1, cCol);
        g.fill(px + 1, py + panelH - 1 - cLen, px + 2, py + panelH - 1, cCol);
        g.fill(px + panelW - 1 - cLen, py + panelH - 2, px + panelW - 1, py + panelH - 1, cCol);
        g.fill(px + panelW - 2, py + panelH - 1 - cLen, px + panelW - 1, py + panelH - 1, cCol);

        // ---- 标题 ----
        String title = stack.getHoverName().getString();
        g.drawString(font, title, px + (panelW - font.width(title)) / 2, py + 7, C_TITLE, true);
        {   // 标题下的一根细线，视觉上"托住"标题
            int tw = font.width(title);
            int lx0 = px + (panelW - tw) / 2 - 12;
            int lx1 = px + (panelW + tw) / 2 + 12;
            g.fill(lx0, py + 19, lx1, py + 20, 0x55C9A6FF);
            g.fill(lx0, py + 20, lx1, py + 21, 0x18C9A6FF);
        }

        if (rows == null) {
            drawRing(g, font, px, py, stack);
            String hint = Component.translatable("sevencurses.tip.hint").getString();
            g.drawString(font, hint, px + (panelW - font.width(hint)) / 2, py + panelH - 14, C_HINT, false);
        } else {
            int y = py + 22;
            for (String s : rows) {
                g.drawString(font, s, px + (panelW - font.width(s)) / 2, y, rowColor, false);
                y += 10;
            }
        }

        pose.popPose();
    }

    /* ================================================================== */

    private static void drawRing(GuiGraphics g, Font font, int px, int py, ItemStack stack) {
        int cx = px + SIZE / 2;
        int cy = py + SIZE / 2 + 4;
        int rIn = (int) Math.round(SIZE * 0.170D);
        int rOut = (int) Math.round(SIZE * 0.380D);

        // 中心：戒指本体
        g.renderItem(stack, cx - 8, cy - 8);

        // 呼吸相位：咒从内往外涨、福从外往里收（严格反相）
        double ph = (tSec / CYCLE) * Math.PI * 2.0D;
        double cosv = Math.cos(ph);
        double pCurse = (1.0D - cosv) * 0.5D;
        double pBoon = (1.0D + cosv) * 0.5D;

        double rCurse = rIn + (rOut - rIn) * pCurse;
        double rBoon = rIn + (rOut - rIn) * pBoon;

        for (int i = 0; i < 7; i++) {
            drawSigil(g, font, cx, cy, rCurse,
                    angBase + i * GAP, CurseType.values()[i].translationKey(), C_CURSE, pCurse);
            drawSigil(g, font, cx, cy, rBoon,
                    angBase + i * GAP + HALF_GAP, BoonType.values()[i].translationKey(), C_BOON, pBoon);
        }
    }

    /** 画一个字：极坐标定位，字保持水平（旋转的中文没法读），越靠外越亮。 */
    private static void drawSigil(GuiGraphics g, Font font, int cx, int cy, double r,
                                  double deg, String key, int color, double depth) {
        String s = firstGlyph(Component.translatable(key).getString());
        double rad = Math.toRadians(deg);
        int x = (int) Math.round(cx + Math.cos(rad) * r - font.width(s) / 2.0D);
        int y = (int) Math.round(cy + Math.sin(rad) * r - font.lineHeight / 2.0D);

        int alpha = (int) Math.round(0x55 + 0xAA * Math.max(0, Math.min(1, depth)));
        int argb = (alpha << 24) | (color & 0x00FFFFFF);
        g.drawString(font, s, x, y, argb, true);
    }

    /* ================================================================== */

    private static boolean altDown() {
        long win = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static List<String> curseRows() {
        List<String> out = new ArrayList<>();
        for (CurseType t : CurseType.values()) {
            out.add(Component.translatable(t.translationKey()).getString()
                    + " " + Component.translatable(t.translationKey() + ".short").getString());
        }
        return out;
    }

    private static List<String> boonRows() {
        List<String> out = new ArrayList<>();
        for (BoonType b : BoonType.values()) {
            out.add(Component.translatable(b.translationKey()).getString()
                    + " " + Component.translatable(b.translationKey() + ".short").getString());
        }
        return out;
    }

    /** 只取第一个「字」（unicode 码点，遇到代理对也不会截断）。 */
    private static String firstGlyph(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        int cp = s.codePointAt(0);
        return new String(Character.toChars(cp));
    }

    /** 两个 RGB 之间插值，返回不透明的 ARGB。 */
    private static int colorMix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 0xFF) + ((((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t));
        int gg = (int) (((a >> 8) & 0xFF) + ((((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t));
        int bb = (int) ((a & 0xFF) + (((b & 0xFF) - (a & 0xFF)) * t));
        return 0xFF000000 | (r << 16) | (gg << 8) | bb;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
