package com.caelestis.client.modules;

import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_3675;
import net.minecraft.class_746;

/**
 * Presses the jump key shortly after you are hit while grounded. The chance slider controls how often an attempt
 * is made (0 = never, 100 = every eligible hit); it does not guarantee that the jump reduces knockback, because
 * the server decides the knockback result.
 */
public class AutoJumpReset extends Module {
    private final NumberSetting chance = add(new NumberSetting("Jump Reset Chance", "Probability (0-100%) of attempting a jump reset on an eligible hit.", 100, 0, 100, 1, "%"));
    private final NumberSetting delay = add(new NumberSetting("Reaction Delay", "Delay between taking the hit and jumping.", 0, 0, 200, 5, " ms"));
    private final BoolSetting projectiles = add(new BoolSetting("Projectiles", "Also react to hits that did not come from a nearby player (arrows, tridents...).", false));

    private int lastHurt = 0;
    private boolean pending = false;
    private long jumpAt = 0;
    private long lastJump = 0;
    private boolean holding = false;

    public AutoJumpReset() {
        super("Auto Jump Reset", "Jumps as you take a hit to try to cut knockback. Chance and delay are adjustable.", Category.COMBAT, 80);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null) return;

        if (holding) { // release the key after exactly one tick unless the real key is held
            holding = false;
            mc.field_1690.field_1903.method_23481(physicalJumpDown());
        }

        int hurt = p.field_6235;
        if (hurt > lastHurt && hurt > 0 && eligibleSource(p)) {
            if (chance.get() > 0 && ThreadLocalRandom.current().nextDouble() * 100.0 < chance.get()) {
                pending = true;
                jumpAt = System.currentTimeMillis() + (long) delay.get();
            }
        }
        lastHurt = hurt;

        if (pending && System.currentTimeMillis() >= jumpAt) {
            pending = false;
            long now = System.currentTimeMillis();
            // Only when grounded, and never spam jumps.
            if (p.method_24828() && !p.method_5799() && !p.method_5771() && !p.method_5765() && now - lastJump > 350) {
                mc.field_1690.field_1903.method_23481(true);
                holding = true;
                lastJump = now;
            }
        }
    }

    private boolean eligibleSource(class_746 p) {
        List<class_1297> near = mc.field_1687.method_8333(p, p.method_5829().method_1014(7.0),
                e -> e instanceof class_1657 pe && pe.method_5805() && !pe.method_7325());
        if (!near.isEmpty()) return true;
        if (!projectiles.get()) return false;
        return true; // hit with no player in melee range: treat as a projectile / ranged hit
    }

    private boolean physicalJumpDown() {
        try {
            class_3675.class_306 key = class_3675.method_15981(mc.field_1690.field_1903.method_1428());
            if (key.method_1442() == class_3675.class_307.field_1668) {
                return GLFW.glfwGetKey(mc.method_22683().method_4490(), key.method_1444()) == GLFW.GLFW_PRESS;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void onDisable() {
        if (holding) mc.field_1690.field_1903.method_23481(physicalJumpDown());
        holding = false;
        pending = false;
    }

    @Override
    public void onWorldChange() {
        holding = false;
        pending = false;
        lastHurt = 0;
    }
}
