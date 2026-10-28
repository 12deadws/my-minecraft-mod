package com.caelestis.client.hud;

import com.caelestis.client.CaelestisClient;
import com.caelestis.client.module.Module;
import com.caelestis.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Branding watermark and enabled-module list. (The totem counter is drawn by the Auto Totem module.) */
public final class Hud {
    private static final int PURPLE = 0xFFC084FC;
    private static final int PURPLE_DIM = 0xFFB88920;

    private Hud() {
    }

    public static void render(DrawContext ctx, ModuleManager mm) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;

        if (mm.clientSettings.watermark.get()) {
            String name = CaelestisClient.NAME;
            int w = tr.getWidth(name) + 10;
            ctx.fill(2, 2, 2 + w, 14, 0xDDF8F1E0);
            ctx.fill(2, 2, 4, 14, PURPLE_DIM);
            ctx.drawText(tr, name, 8, 4, PURPLE, true);
        }

        if (mm.clientSettings.moduleList.get()) {
            List<String> lines = new ArrayList<>();
            for (Module m : mm.all()) {
                if (!m.toggleable || !m.isEnabled()) continue;
                String info = m.hudInfo();
                lines.add(info == null || info.isEmpty() ? m.name : m.name + " §7" + info);
            }
            lines.sort(Comparator.comparingInt((String s) -> tr.getWidth(s)).reversed());
            int y = 3;
            int right = ctx.getScaledWindowWidth() - 3;
            for (String s : lines) {
                int w = tr.getWidth(s);
                ctx.fill(right - w - 6, y - 1, right + 1, y + 10, 0xCCF8F1E0);
                ctx.fill(right, y - 1, right + 1, y + 10, PURPLE_DIM);
                ctx.drawText(tr, s, right - w - 3, y + 1, PURPLE, true);
                y += 11;
            }
        }
    }
}
