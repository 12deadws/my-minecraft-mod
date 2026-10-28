package com.caelestis.client.modules;

import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        if (holding) { // release the key after exactly one tick unless the real key is held
            holding = false;
            mc.options.jumpKey.setPressed(physicalJumpDown());
        }

        int hurt = p.hurtTime;
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
            if (p.isOnGround() && !p.isTouchingWater() && !p.isInLava() && !p.hasVehicle() && now - lastJump > 350) {
                mc.options.jumpKey.setPressed(true);
                holding = true;
                lastJump = now;
            }
        }
    }

    private boolean eligibleSource(ClientPlayerEntity p) {
        List<Entity> near = mc.world.getOtherEntities(p, p.getBoundingBox().expand(7.0),
                e -> e instanceof PlayerEntity pe && pe.isAlive() && !pe.isSpectator());
        if (!near.isEmpty()) return true;
        if (!projectiles.get()) return false;
        return true; // hit with no player in melee range: treat as a projectile / ranged hit
    }

    private boolean physicalJumpDown() {
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey());
            if (key.getCategory() == InputUtil.Type.KEYSYM) {
                return GLFW.glfwGetKey(mc.getWindow().getHandle(), key.getCode()) == GLFW.GLFW_PRESS;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void onDisable() {
        if (holding) mc.options.jumpKey.setPressed(physicalJumpDown());
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
