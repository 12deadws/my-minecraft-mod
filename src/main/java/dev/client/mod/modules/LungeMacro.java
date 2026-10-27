package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

/** While enabled: swaps to a spear and jabs whatever you're aiming at on cooldown (spear lunge). */
public class LungeMacro extends Module {
    public LungeMacro() {
        super("Lunge", GLFW.GLFW_KEY_H, false);
        settings.add(Setting.info("Needs", () -> "spear in hotbar"));
    }

    @Override public void onTick() {
        if (!Util.select(s -> s.is(ItemTags.SPEARS))) return;
        if (!Util.ready()) return;
        Entity t = Util.target(Util.reach() + 1.0);
        if (t != null) Util.attack(t);
    }
}
