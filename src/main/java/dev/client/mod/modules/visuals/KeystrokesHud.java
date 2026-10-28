package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** WASD, mouse buttons and space, lit up while held. Left side of the screen. */
public class KeystrokesHud extends Module {
    public KeystrokesHud() { super("Keystrokes", -1, false, "Visuals"); }

    private static void key(GuiGraphics g, int x, int y, int w, int h, String label, boolean down) {
        Draw.rect(g, x, y, w, h, down ? 0xB05AC8FF : Draw.BG);
        Draw.text(g, label, x + (w - Draw.width(label)) / 2, y + (h - 8) / 2, Draw.TEXT);
    }

    @Override public void onRender(GuiGraphics g) {
        Minecraft mc = Util.mc;
        int s = 20, gap = 2;
        int x0 = 8, y0 = g.guiHeight() / 2 - 25;
        int total = 3 * s + 2 * gap;
        int half = (total - gap) / 2;

        key(g, x0 + s + gap, y0, s, s, "W", mc.options.keyUp.isDown());
        key(g, x0, y0 + s + gap, s, s, "A", mc.options.keyLeft.isDown());
        key(g, x0 + s + gap, y0 + s + gap, s, s, "S", mc.options.keyDown.isDown());
        key(g, x0 + 2 * (s + gap), y0 + s + gap, s, s, "D", mc.options.keyRight.isDown());

        int y2 = y0 + 2 * (s + gap);
        key(g, x0, y2, half, 16, "LMB", mc.options.keyAttack.isDown());
        key(g, x0 + half + gap, y2, half, 16, "RMB", mc.options.keyUse.isDown());
        key(g, x0, y2 + 18, total, 12, "SPACE", mc.options.keyJump.isDown());
    }
}
