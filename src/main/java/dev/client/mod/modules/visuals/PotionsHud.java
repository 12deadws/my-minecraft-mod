package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Active potion effects with level and time left. Under the Info HUD on the left. */
public class PotionsHud extends Module {
    public PotionsHud() { super("Potions HUD", -1, false, "Visuals"); }

    @Override public void onRender(GuiGraphics g) {
        LocalPlayer p = Util.mc.player;
        if (p == null) return;
        Collection<MobEffectInstance> effects = p.getActiveEffects();
        if (effects.isEmpty()) return;

        List<String> lines = new ArrayList<>();
        lines.add("Effects");
        for (MobEffectInstance e : effects) {
            String name = Component.translatable(e.getDescriptionId()).getString();
            String time;
            if (e.isInfiniteDuration()) {
                time = "inf";
            } else {
                int secs = e.getDuration() / 20;
                time = String.format(Locale.ROOT, "%d:%02d", secs / 60, secs % 60);
            }
            lines.add(name + " " + (e.getAmplifier() + 1) + "  " + time);
        }
        Draw.panel(g, 4, 46, lines.toArray(new String[0]));
    }
}
