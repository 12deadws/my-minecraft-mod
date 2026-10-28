package dev.client.mod.gui;

import dev.client.mod.Config;
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
 * Click the Keybind row, press a key to bind it, Esc clears the bind. Right Shift is reserved.
 * Mouse and key input are polled from GLFW in render() so this doesn't depend on input-event signatures.
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
    private Module listening = null;                 // module whose keybind is being set
    private boolean lmbWas;
    private boolean shiftWas = true;                 // Right Shift is still down from opening the menu
    private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private String notice = "";

    public ClickGuiScreen() {
        super(Component.literal("Caelestial Client"));
    }

    private long win() { return Minecraft.getInstance().getWindow().handle(); }

    private boolean lmb() {
        return GLFW.glfwGetMouseButton(win(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @Override protected void init() { lmbWas = lmb(); }

    @Override public boolean isPauseScreen() { return false; }

    /** While waiting for a key, Esc must clear the bind instead of closing the menu. */
    @Override public boolean shouldCloseOnEsc() { return listening == null; }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String keyName(int key) {
        if (key < 0) return "NONE";
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "L-ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "R-ALT";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACKSPACE";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PAGE UP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PAGE DOWN";
            default: break;
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(key, 0);
        return n == null ? "KEY " + key : n.toUpperCase(Locale.ROOT);
    }

    private void bind(int key) {
        Module m = listening;
        listening = null;
        if (m == null) return;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            m.key = -1;
            notice = m.name + " keybind cleared";
        } else {
            // Conflict: a key can only run one module, so the previous owner is unbound
            String freed = null;
            for (Module o : ModuleManager.modules) {
                if (o != m && o.key == key) { o.key = -1; freed = o.name; }
            }
            m.key = key;
            notice = m.name + " bound to " + keyName(key) + (freed != null ? " (unbound from " + freed + ")" : "");
        }
        Config.save();
    }

    @Override
    public void onClose() {
        listening = null;
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Right Shift: cancels key-binding if waiting for a key, otherwise closes the menu
        boolean shift = GLFW.glfwGetKey(win(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (shift && !shiftWas) {
            if (listening != null) {
                listening = null;
                notice = "Keybind change cancelled";
            } else {
                shiftWas = shift;
                onClose();
                return;
            }
        }
        shiftWas = shift;

        // Key capture
        if (listening != null) {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (k == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
                boolean now = GLFW.glfwGetKey(win(), k) == GLFW.GLFW_PRESS;
                if (now && !prevKeys[k]) { bind(k); break; }
            }
        }
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            prevKeys[k] = GLFW.glfwGetKey(win(), k) == GLFW.GLFW_PRESS;
        }

        boolean down = lmb();
        boolean clicked = down && !lmbWas;
        lmbWas = down;

        g.fill(0, 0, width, height, DIM);

        int x = (width - W) / 2;
        int y = 24;

        Component title = Component.literal("Caelestial Client").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, (width - font.width(title)) / 2, y, ACCENT);
        y += 16;

        Module nextOpen = open;
        Module nextListening = listening;
        boolean keyRowClicked = false;

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
                // Keybind row (click, then press a key; Esc clears)
                boolean overKey = inside(mx, my, x, y, W, SROW);
                g.fill(x, y, x + W, y + SROW, overKey ? ROW_HOVER : SUB_BG);
                g.drawString(font, "Keybind", x + PAD + 6, y + 4, MUTED);
                boolean waiting = listening == m;
                String k = waiting ? "Press any key..." : keyName(m.key);
                g.drawString(font, k, x + W - PAD - font.width(k), y + 4, waiting ? ACCENT : TEXT);
                if (clicked && overKey) {
                    nextListening = waiting ? null : m;
                    keyRowClicked = true;
                }
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
        if (clicked && !keyRowClicked) nextListening = null;   // clicking anywhere else cancels
        listening = nextListening;

        String hint = "Right Shift to close";
        g.drawString(font, hint, (width - font.width(hint)) / 2, y + 6, MUTED);
        if (!notice.isEmpty()) {
            g.drawString(font, notice, (width - font.width(notice)) / 2, y + 18, ACCENT);
        }
    }
}
