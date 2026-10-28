package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

/** Name and health bar of whatever you're aiming at (within 8 blocks). */
public class TargetHud extends Module {
    public TargetHud() { super("Target HUD", -1, false, "Visuals"); }

    @Override public void onRender(GuiGraphics g) {
        Entity e = Util.target(8.0);
        if (!(e instanceof LivingEntity le)) return;

        float hp = le.getHealth();
        float max = Math.max(1f, le.getMaxHealth());
        float frac = Math.max(0f, Math.min(1f, hp / max));

        int w = 120, h = 32;
        int x = g.guiWidth() / 2 + 24, y = g.guiHeight() / 2 + 14;
        Draw.rect(g, x, y, w, h, Draw.BG);
        Draw.rect(g, x, y, 2, h, Draw.ACCENT);
        Draw.text(g, le.getName().getString(), x + 7, y + 4, Draw.TEXT);

        int bx = x + 7, by = y + 15, bw = w - 14;
        Draw.rect(g, bx, by, bw, 5, 0xFF333333);
        int color = frac > 0.5f ? 0xFF6BFF9A : (frac > 0.25f ? 0xFFFFD66B : 0xFFFF6B6B);
        Draw.rect(g, bx, by, (int) (bw * frac), 5, color);
        Draw.text(g, String.format(Locale.ROOT, "%.1f / %.1f", hp, max), x + 7, y + 22, Draw.MUTED);
    }
}
