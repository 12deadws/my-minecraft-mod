package dev.client.mod.gui;

import dev.client.mod.Binds;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Caelestial Client menu. Opened with Right Shift, closed with Right Shift or Esc.
 * Modules are grouped into one column per category (Combat, Visuals, ...).
 * Click a module to open its dropdown (one open at a time), click ON/OFF to toggle it.
 * Mouse input is polled from GLFW in render() so this doesn't depend on the mouse-event method signatures.
 */
public class ClickGuiScreen extends Screen {
    private static final int W = 200, GAP = 10, ROW = 18, SROW = 16, PAD = 6, HEAD = 16;

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
    private Module nextOpen = null;
    private int colW = W;

    // Keybind capture: click a Key row, press a key. Esc cancels, Backspace/Delete clears.
    private Binds.Target listening = null;
    private final boolean[] held = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private String notice = "";
    private long noticeUntil = 0;
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

    /** While waiting for a key, Esc must cancel the capture instead of closing the menu. */
    @Override public boolean shouldCloseOnEsc() { return listening == null; }

    private boolean isListening(Binds.Target t) { return listening != null && listening.id().equals(t.id()); }

    private void startListening(Binds.Target t) {
        long w = win();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) held[k] = GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
        listening = isListening(t) ? null : t;   // clicking the same row again cancels
    }

    /** Takes the first key that goes down (ignoring keys already held when capture started). */
    private void pollBind() {
        long w = win();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            boolean pressed = GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
            boolean edge = pressed && !held[k];
            held[k] = pressed;
            if (!edge || listening == null) continue;

            if (k == GLFW.GLFW_KEY_ESCAPE) {
                listening = null;
            } else {
                int bind = (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) ? -1 : k;
                Binds.Result r = Binds.assign(listening, bind);
                notice = r.msg();
                noticeUntil = System.currentTimeMillis() + 3000;
                listening = null;
                shiftWas = true;   // don't treat a freshly bound menu key as "pressed to close"
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        // Close on Right Shift
        boolean shift = GLFW.glfwGetKey(win(), Config.menuKey) == GLFW.GLFW_PRESS;
        if (listening == null && shift && !shiftWas) { onClose(); return; }
        shiftWas = shift;
        if (listening != null) pollBind();

        boolean down = lmb();
        boolean clicked = down && !lmbWas;
        lmbWas = down;

        g.fill(0, 0, width, height, DIM);

        // Title
        Component title = Component.literal("Caelestial Client").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, (width - font.width(title)) / 2, 10, ACCENT);

        // One column per category, in the order they first appear in ModuleManager.
        List<String> cats = new ArrayList<>();
        for (Module m : ModuleManager.modules) if (!cats.contains(m.category)) cats.add(m.category);

        int n = Math.max(1, cats.size());
        colW = Math.max(170, Math.min(W, (width - 20 - (n - 1) * GAP) / n));
        int x = (width - (n * colW + (n - 1) * GAP)) / 2;

        nextOpen = open;
        int bottom = 0;
        boolean first = true;
        for (String cat : cats) {
            int end = drawColumn(g, cat, x, 28, mx, my, clicked);
            if (first) end = drawClientKeys(g, x, end + 8, mx, my, clicked);   // fills the space under the first column
            first = false;
            bottom = Math.max(bottom, end);
            x += colW + GAP;
        }
        open = nextOpen;

        String hint = listening != null
                ? "Press a key to bind it  (Esc cancels, Backspace clears)"
                : Binds.keyName(Config.menuKey) + " to close  |  click a Key row to rebind";
        g.drawString(font, hint, (width - font.width(hint)) / 2, bottom + 6, MUTED);
        if (System.currentTimeMillis() < noticeUntil) {
            g.drawString(font, notice, (width - font.width(notice)) / 2, bottom + 18, ACCENT);
        }
    }

    /** Client-wide keys (menu, hit chance +/-). Returns the y just below the block. */
    private int drawClientKeys(GuiGraphics g, int x, int y, int mx, int my, boolean clicked) {
        final int W = colW;
        g.fill(x, y, x + W, y + HEAD, PANEL);
        Component head = Component.literal("Client keys").withStyle(ChatFormatting.BOLD);
        g.drawString(font, head, x + (W - font.width(head)) / 2, y + 4, ACCENT);
        g.fill(x, y + HEAD, x + W, y + HEAD + 1, ACCENT);
        y += HEAD + 2;

        for (Binds.Target t : Binds.client()) {
            boolean hover = inside(mx, my, x, y, W, ROW);
            g.fill(x, y, x + W, y + ROW, hover ? ROW_HOVER : ROW_BG);
            g.drawString(font, t.label(), x + PAD, y + 5, TEXT);
            boolean waiting = isListening(t);
            String k = waiting ? "Press a key..." : Binds.keyName(t.get().getAsInt());
            g.drawString(font, k, x + W - PAD - font.width(k), y + 5, waiting ? ACCENT : MUTED);
            if (clicked && hover) startListening(t);
            y += ROW + 1;
        }
        return y;
    }

    /** Draws one category column and returns the y just below it. */
    private int drawColumn(GuiGraphics g, String cat, int x, int y, int mx, int my, boolean clicked) {
        final int W = colW;

        // Header
        g.fill(x, y, x + W, y + HEAD, PANEL);
        Component head = Component.literal(cat).withStyle(ChatFormatting.BOLD);
        g.drawString(font, head, x + (W - font.width(head)) / 2, y + 4, ACCENT);
        g.fill(x, y + HEAD, x + W, y + HEAD + 1, ACCENT);
        y += HEAD + 2;

        for (Module m : ModuleManager.modules) {
            if (!m.category.equals(cat)) continue;

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
                // Key row: click, then press the new key
                Binds.Target kt = Binds.forModule(m);
                boolean overKey = inside(mx, my, x, y, W, SROW);
                boolean waiting = isListening(kt);
                g.fill(x, y, x + W, y + SROW, overKey ? ROW_HOVER : SUB_BG);
                g.drawString(font, "Key", x + PAD + 6, y + 4, MUTED);
                String k = waiting ? "Press a key..." : Binds.keyName(m.key);
                g.drawString(font, k, x + W - PAD - font.width(k), y + 4, waiting ? ACCENT : TEXT);
                if (clicked && overKey) startListening(kt);
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
        return y;
    }
}
