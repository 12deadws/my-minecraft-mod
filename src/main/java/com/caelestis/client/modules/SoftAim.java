package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.EnumSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.InvUtil;
import com.caelestis.client.util.ItemUtil;
import com.caelestis.client.util.Targets;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Smooth, additive aim assist. Target selection runs each tick; the camera is nudged every rendered frame with
 * exponential smoothing, through the same code path as mouse movement (Entity.changeLookDirection), so normal
 * mouse input keeps working at the same time.
 */
public class SoftAim extends Module {
    private final NumberSetting strength = add(new NumberSetting("Aim Strength", "How much of the remaining angle is corrected (0-100%).", 50, 0, 100, 1, "%"));
    private final NumberSetting smooth = add(new NumberSetting("Aim Smoothness", "Higher = slower, softer tracking (1-100).", 40, 1, 100, 1));
    private final NumberSetting range = add(new NumberSetting("Aim Range", "Maximum distance to a target.", 6.0, 2.0, 12.0, 0.5, " m"));
    private final NumberSetting fov = add(new NumberSetting("Aim FOV", "Only targets within this cone around your view (degrees).", 90, 1, 180, 1, "°"));
    private final EnumSetting aimPoint = add(new EnumSetting("Target", "Where on the player to aim.", "Chest", "Head", "Chest"));
    private final EnumSetting priority = add(new EnumSetting("Priority", "How to choose between several targets.", "Crosshair", "Closest", "Crosshair"));
    private final BoolSetting onlyAttacking = add(new BoolSetting("Only While Attacking", "Only assist while the attack key is held.", false));
    private final BoolSetting onlyWeapon = add(new BoolSetting("Only While Holding Weapon", "Only assist with a sword, axe or mace in hand.", true));

    private PlayerEntity target;

    public SoftAim() {
        super("Soft Aim", "Gently pulls your camera toward the best enemy in view. No snapping, no wall tracking.", Category.COMBAT, 90);
    }

    public PlayerEntity getTarget() {
        return target;
    }

    @Override
    public void onTick() {
        target = null;
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        if (onlyAttacking.get() && !mc.options.attackKey.isPressed()) return;
        if (onlyWeapon.get() && !holdingWeapon(p.getMainHandStack())) return;

        // Stay on the player TriggerBot is hitting, if it is still valid and in view.
        PlayerEntity preferred = Targets.getPreferred();
        if (preferred != null && valid(p, preferred)) {
            target = preferred;
            return;
        }

        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (PlayerEntity e : Targets.enemies(range.get())) {
            if (!valid(p, e)) continue;
            double score = priority.is("Closest") ? p.distanceTo(e) : angleTo(p, e);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        target = best;
    }

    @Override
    public void onFrame(float dt) {
        ClientPlayerEntity p = mc.player;
        if (p == null || target == null || mc.currentScreen != null) return;
        if (strength.get() <= 0.0) return;
        if (!ActionManager.isFree(ActionManager.Channel.ROTATION, this)) return;
        if (!valid(p, target)) return;

        Vec3d aim = aimPoint(target);
        Vec3d eye = p.getEyePos();
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));

        float dYaw = MathHelper.wrapDegrees(wantYaw - p.getYaw());
        float dPitch = wantPitch - p.getPitch();

        // Exponential smoothing: rate falls from 25/s (smoothness 1) to 1.5/s (smoothness 100).
        double rate = MathHelper.lerp((smooth.get() - 1.0) / 99.0, 25.0, 1.5);
        double frac = (1.0 - Math.exp(-rate * Math.min(dt, 0.1f))) * (strength.get() / 100.0);

        float stepYaw = (float) (dYaw * frac);
        float stepPitch = (float) (dPitch * frac);
        // changeLookDirection takes mouse-delta units (x0.15 degrees) and keeps interpolation smooth.
        p.changeLookDirection(stepYaw / 0.15, stepPitch / 0.15);
    }

    private boolean valid(ClientPlayerEntity p, PlayerEntity e) {
        if (!Targets.isValidEnemy(e)) return false;
        if (p.distanceTo(e) > range.get()) return false;
        if (!Targets.canSee(e)) return false; // never track through walls
        return angleTo(p, e) <= fov.get() / 2.0;
    }

    private Vec3d aimPoint(PlayerEntity e) {
        double y = aimPoint.is("Head") ? e.getY() + e.getStandingEyeHeight() : e.getY() + e.getHeight() * 0.65;
        return new Vec3d(e.getX(), y, e.getZ());
    }

    private double angleTo(ClientPlayerEntity p, PlayerEntity e) {
        Vec3d aim = aimPoint(e);
        Vec3d eye = p.getEyePos();
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));
        double a = MathHelper.wrapDegrees(yaw - p.getYaw());
        double b = pitch - p.getPitch();
        return Math.sqrt(a * a + b * b);
    }

    private boolean holdingWeapon(ItemStack s) {
        return ItemUtil.isSword(s) || ItemUtil.isAxe(s) || ItemUtil.isMace(s);
    }

    @Override
    public void onWorldChange() {
        target = null;
    }

    @Override
    public void onDisable() {
        target = null;
    }
}
