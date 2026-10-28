package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Abilities;
import org.lwjgl.glfw.GLFW;

/** Creative-style flight. Server must have allow-flight=true or it will kick you. */
public class Fly extends Module {
    public Fly() { super("Fly", -1, false);   // no default key: toggle from the menu or bind one there }

    @Override public void onTick() {
        Abilities a = Minecraft.getInstance().player.getAbilities();
        a.mayfly = true;
        a.flying = true;
    }

    @Override public void onDisable() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Abilities a = mc.player.getAbilities();
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            a.mayfly = false;
            a.flying = false;
        }
    }
}
