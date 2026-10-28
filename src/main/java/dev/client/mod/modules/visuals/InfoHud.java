package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

/** Top-left watermark with coordinates and FPS. */
public class InfoHud extends Module {
    public InfoHud() {
        super("Info HUD", -1, false, "Visuals");
        settings.add(Setting.info("Shows", () -> "name, XYZ, FPS"));
    }

    @Override public void onRender(GuiGraphics g) {
        Minecraft mc = Util.mc;
        LocalPlayer p = mc.player;
        if (p == null) return;
        String[] lines = {
                "Caelestial",
                String.format(Locale.ROOT, "XYZ  %.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ()),
                "FPS  " + mc.getFps()
        };
        Draw.panel(g, 4, 4, lines);
    }
}
