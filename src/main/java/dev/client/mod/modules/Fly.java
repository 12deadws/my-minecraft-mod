package dev.client.mod.modules;

import dev.client.mod.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Abilities;
import org.lwjgl.glfw.GLFW;

/** Creative-style flight. Server must have allow-flight=true or it will kick you. */
public class Fly extends Module {
    public Fly() { super("Fly", GLFW.GLFW_KEY_F, false); }

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
