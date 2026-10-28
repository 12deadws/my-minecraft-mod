package dev.client.mod;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Tiny 2D drawing helpers for the HUD modules. */
public final class Draw {
    public static final int BG = 0x90000000;
    public static final int ACCENT = 0xFF5AC8FF;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFF9AA3B8;
    public static final int LINE = 10;   // line height

    private Draw() {}

    public static Font font() { return Util.mc.font; }
    public static int width(String s) { return font().width(s); }

    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color);
    }

    public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    /** Translucent panel with an accent bar; first line is drawn in the accent colour. Returns the panel height. */
    public static int panel(GuiGraphics g, int x, int y, String[] lines) {
        int maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, width(l));
        int w = maxW + 12, h = lines.length * LINE + 6;
        rect(g, x, y, w, h, BG);
        rect(g, x, y, 2, h, ACCENT);
        for (int i = 0; i < lines.length; i++) {
            text(g, lines[i], x + 7, y + 4 + i * LINE, i == 0 ? ACCENT : TEXT);
        }
        return h;
    }
}
