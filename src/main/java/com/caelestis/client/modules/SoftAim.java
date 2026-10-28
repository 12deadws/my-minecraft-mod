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
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_746;

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

    private class_1657 target;

    public SoftAim() {
        super("Soft Aim", "Gently pulls your camera toward the best enemy in view. No snapping, no wall tracking.", Category.COMBAT, 90);
    }

    public class_1657 getTarget() {
        return target;
    }

    @Override
    public void onTick() {
        target = null;
        class_746 p = mc.field_1724;
        if (p == null) return;
        if (onlyAttacking.get() && !mc.field_1690.field_1886.method_1434()) return;
        if (onlyWeapon.get() && !holdingWeapon(p.method_6047())) return;

        // Stay on the player TriggerBot is hitting, if it is still valid and in view.
        class_1657 preferred = Targets.getPreferred();
        if (preferred != null && valid(p, preferred)) {
            target = preferred;
            return;
        }

        class_1657 best = null;
        double bestScore = Double.MAX_VALUE;
        for (class_1657 e : Targets.enemies(range.get())) {
            if (!valid(p, e)) continue;
            double score = priority.is("Closest") ? p.method_5739(e) : angleTo(p, e);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        target = best;
    }

    @Override
    public void onFrame(float dt) {
        class_746 p = mc.field_1724;
        if (p == null || target == null || mc.field_1755 != null) return;
        if (strength.get() <= 0.0) return;
        if (!ActionManager.isFree(ActionManager.Channel.ROTATION, this)) return;
        if (!valid(p, target)) return;

        class_243 aim = aimPoint(target);
        class_243 eye = p.method_33571();
        double dx = aim.field_1352 - eye.field_1352, dy = aim.field_1351 - eye.field_1351, dz = aim.field_1350 - eye.field_1350;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));

        float dYaw = class_3532.method_15393(wantYaw - p.method_36454());
        float dPitch = wantPitch - p.method_36455();

        // Exponential smoothing: rate falls from 25/s (smoothness 1) to 1.5/s (smoothness 100).
        double rate = class_3532.method_16436((smooth.get() - 1.0) / 99.0, 25.0, 1.5);
        double frac = (1.0 - Math.exp(-rate * Math.min(dt, 0.1f))) * (strength.get() / 100.0);

        float stepYaw = (float) (dYaw * frac);
        float stepPitch = (float) (dPitch * frac);
        // changeLookDirection takes mouse-delta units (x0.15 degrees) and keeps interpolation smooth.
        p.method_5872(stepYaw / 0.15, stepPitch / 0.15);
    }

    private boolean valid(class_746 p, class_1657 e) {
        if (!Targets.isValidEnemy(e)) return false;
        if (p.method_5739(e) > range.get()) return false;
        if (!Targets.canSee(e)) return false; // never track through walls
        return angleTo(p, e) <= fov.get() / 2.0;
    }

    private class_243 aimPoint(class_1657 e) {
        double y = aimPoint.is("Head") ? e.method_23318() + e.method_5751() : e.method_23318() + e.method_17682() * 0.65;
        return new class_243(e.method_23317(), y, e.method_23321());
    }

    private double angleTo(class_746 p, class_1657 e) {
        class_243 aim = aimPoint(e);
        class_243 eye = p.method_33571();
        double dx = aim.field_1352 - eye.field_1352, dy = aim.field_1351 - eye.field_1351, dz = aim.field_1350 - eye.field_1350;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));
        double a = class_3532.method_15393(yaw - p.method_36454());
        double b = pitch - p.method_36455();
        return Math.sqrt(a * a + b * b);
    }

    private boolean holdingWeapon(class_1799 s) {
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
