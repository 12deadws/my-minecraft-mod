package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * Press G while aiming at a target:
 *  1. swap to axe, hit (disables shield)
 *  2. swap to mace
 *  3. when falling and cooldown is back, smash
 * Start it while already in the air (wind charge / jump off a block).
 */
public class StunSlam extends Module {
    private int state = 0, timer = 0;
    private Entity target;

    public StunSlam() {
        super("StunSlam", GLFW.GLFW_KEY_G, true);
        settings.add(Setting.info("Usage", () -> "aim at target, press key"));
    }

    @Override public void activate() {
        if (state != 0) return;
        target = Util.target(Util.reach() + 2.0);
        if (target != null) { state = 1; timer = 0; }
    }

    @Override public void onTick() {
        if (state == 0) return;
        LocalPlayer p = Util.mc.player;
        if (target == null || !target.isAlive() || ++timer > 80) { state = 0; return; }
        double dist = p.distanceTo(target);

        switch (state) {
            case 1 -> {
                if (!Util.select(s -> s.is(ItemTags.AXES))) { state = 0; return; }
                if (dist <= Util.reach() && Util.ready()) { Util.attack(target); state = 2; }
            }
            case 2 -> {
                if (!Util.select(s -> s.is(Items.MACE))) { state = 0; return; }
                state = 3;
            }
            case 3 -> {
                if (p.fallDistance > 1.5 && dist <= Util.reach() && Util.ready()) {
                    Util.attack(target);
                    state = 0;
                }
            }
        }
    }
}
