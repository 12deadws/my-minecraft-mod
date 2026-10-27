package dev.client.mod.gui;

import dev.client.mod.Module;
import dev.client.mod.ModuleManager;
import dev.client.mod.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Caelestial Client menu. Opened with Right Shift, closed with Right Shift or Esc.
 * Click a module to open its dropdown (one open at a time), click ON/OFF to toggle it.
 * Mouse input is polled from GLFW in render() so this doesn't depend on the mouse-event method signatures.
 */
public class ClickGuiScreen extends Screen {
    private static final int W = 240, ROW = 18, SROW = 16, PAD = 6;

    private static final int DIM = 0x90000000;
    private static final int PANEL = 0xFF1E2230;
    private static final int ROW_BG = 0xFF2A2F42;
    private static final int ROW_HOVER = 0xFF343A52;
    private static final int SUB_BG = 0xFF232738;
    private static final int ACCENT = 0xFF5AC8FF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF9AA3B8;
    private static final int OFF = 0xFFFF6B6B;
    private static final int ON = 0xFF6BFF9A;

    private Module open = null;
    private boolean lmbWas;
    private boolean shiftWas = true;   // Right Shift is still down from opening the menu

    public ClickGuiScreen() {
        super(Component.literal("Caelestial Client"));
    }

    private long win() { return Minecraft.getInstance().getWindow().handle(); }

    private boolean lmb() {
        return GLFW.glfwGetMouseButton(win(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @Override protected void init() { lmbWas = lmb(); }

    @Override public boolean isPauseScreen() { return false; }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        return n == null ? "?" : n.toUpperCase(Locale.ROOT);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Close on Right Shift
        boolean shift = GLFW.glfwGetKey(win(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (shift && !shiftWas) { onClose(); return; }
        shiftWas = shift;

        boolean down = lmb();
        boolean clicked = down && !lmbWas;
        lmbWas = down;

        g.fill(0, 0, width, height, DIM);

        int x = (width - W) / 2;
        int y = 24;

        // Title
        Component title = Component.literal("Caelestial Client").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, (width - font.width(title)) / 2, y, ACCENT);
        y += 16;

        Module nextOpen = open;

        for (Module m : ModuleManager.modules) {
            boolean hover = inside(mx, my, x, y, W, ROW);
            g.fill(x, y, x + W, y + ROW, hover ? ROW_HOVER : ROW_BG);
            if (m.enabled && !m.macro) g.fill(x, y, x + 2, y + ROW, ACCENT);

            g.drawString(font, (open == m ? "- " : "+ ") + m.name, x + PAD, y + 5, TEXT);

            String status = m.macro ? "RUN" : (m.enabled ? "ON" : "OFF");
            int statusColor = m.macro ? ACCENT : (m.enabled ? ON : OFF);
            int bx = x + W - 38, bw = 32;
            boolean overBtn = inside(mx, my, bx, y + 2, bw, ROW - 4);
            g.fill(bx, y + 2, bx + bw, y + ROW - 2, overBtn ? 0xFF444C6B : 0xFF3A4160);
            g.drawString(font, status, bx + (bw - font.width(status)) / 2, y + 5, statusColor);

            if (clicked) {
                if (overBtn) {
                    if (m.macro) m.activate(); else m.toggle();
                } else if (hover) {
                    nextOpen = (open == m) ? null : m;
                }
            }
            y += ROW;

            if (open == m) {
                // Key row
                g.fill(x, y, x + W, y + SROW, SUB_BG);
                g.drawString(font, "Key", x + PAD + 6, y + 4, MUTED);
                String k = keyName(m.key);
                g.drawString(font, k, x + W - PAD - font.width(k), y + 4, TEXT);
                y += SROW;

                for (Setting s : m.settings) {
                    g.fill(x, y, x + W, y + SROW, SUB_BG);
                    g.drawString(font, s.name, x + PAD + 6, y + 4, MUTED);
                    String val = s.value.get();
                    if (s.adjustable()) {
                        int incX = x + W - PAD - 14, decX = x + W - PAD - 14 - 70;
                        boolean overDec = inside(mx, my, decX, y + 1, 14, SROW - 2);
                        boolean overInc = inside(mx, my, incX, y + 1, 14, SROW - 2);
                        g.fill(decX, y + 1, decX + 14, y + SROW - 1, overDec ? 0xFF444C6B : 0xFF3A4160);
                        g.fill(incX, y + 1, incX + 14, y + SROW - 1, overInc ? 0xFF444C6B : 0xFF3A4160);
                        g.drawString(font, "-", decX + 5, y + 4, TEXT);
                        g.drawString(font, "+", incX + 4, y + 4, TEXT);
                        int centerX = decX + 14 + (incX - decX - 14) / 2;
                        g.drawString(font, val, centerX - font.width(val) / 2, y + 4, TEXT);
                        if (clicked && overDec) s.dec.run();
                        if (clicked && overInc) s.inc.run();
                    } else {
                        g.drawString(font, val, x + W - PAD - font.width(val), y + 4, TEXT);
                    }
                    y += SROW;
                }
                g.fill(x, y, x + W, y + 2, PANEL);
                y += 2;
            }
            y += 1;
        }
        open = nextOpen;

        String hint = "Right Shift to close";
        g.drawString(font, hint, (width - font.width(hint)) / 2, y + 6, MUTED);
    }
}
