package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Enabled modules, longest name first, down the top-right. */
public class ArrayListHud extends Module {
    public ArrayListHud() { super("ArrayList", -1, false, "Visuals"); }

    @Override public void onRender(GuiGraphics g) {
        List<Module> on = new ArrayList<>();
        for (Module m : ModuleManager.modules) {
            if (m.enabled && !m.macro && m != this) on.add(m);
        }
        on.sort(Comparator.comparingInt((Module m) -> -Draw.width(m.name)));

        int y = 4;
        for (Module m : on) {
            int w = Draw.width(m.name);
            int x = g.guiWidth() - w - 10;
            Draw.rect(g, x - 3, y, w + 8, Draw.LINE + 1, Draw.BG);
            Draw.rect(g, g.guiWidth() - 3, y, 3, Draw.LINE + 1, Draw.ACCENT);
            Draw.text(g, m.name, x, y + 2, Draw.TEXT);
            y += Draw.LINE + 1;
        }
    }
}
