package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.lwjgl.glfw.GLFW;

/**
 * Raises your entity interaction range, so your own clicks AND the auto modules reach further.
 * The server still checks distance itself (vanilla allows a little over 3 blocks plus a buffer).
 */
public class Reach extends Module {
    private static final double VANILLA = 3.0;

    public Reach() {
        super("Reach", GLFW.GLFW_KEY_J, false);
        settings.add(new Setting("Range", () -> String.format(java.util.Locale.ROOT, "%.2f", Config.reachRange),
                () -> { Config.reachRange = Math.max(3.0, Config.reachRange - 0.25); Config.save(); },
                () -> { Config.reachRange = Math.min(6.0, Config.reachRange + 0.25); Config.save(); }));
    }

    private void apply(double value) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        AttributeInstance a = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (a != null && a.getBaseValue() != value) a.setBaseValue(value);
    }

    @Override public void onTick() { apply(Config.reachRange); }

    @Override public void onDisable() { apply(VANILLA); }
}
