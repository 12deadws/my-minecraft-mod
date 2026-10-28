package dev.cael3stis.client.modules;

import dev.cael3stis.client.Cael3stisClient;
import dev.cael3stis.client.GameContext;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_3489;
import net.minecraft.class_3532;

/** Small user-configured camera movements, limited to line-of-sight players. */
public final class SoftAim extends Module {
    private final Setting.Number strength = number("Aim strength (%)", 22, 0, 100, 2);
    private final Setting.Number smoothness = number("Smoothness", 65, 1, 100, 1);
    private final Setting.Number range = number("Range (blocks)", 4, 1, 6, 1);
    private final Setting.Number fov = number("FOV (degrees)", 45, 1, 180, 5);
    private final Setting.Choice targetPart = choice("Target", 1, "Head", "Chest");
    private final Setting.Toggle onlyWeapon = toggle("Only while holding weapon", true);
    private final Setting.Toggle onlyAttacking = toggle("Only while attacking", true);

    public SoftAim() { super("Soft Aim", "Combat", "Gently follow visible nearby players within the FOV."); }

    @Override public void tick(GameContext c) {
        if (c.client.field_1755 != null || strength.value == 0) return;
        if (onlyAttacking.value && !c.client.field_1690.field_1886.method_1434()) return;
        if (onlyWeapon.value && !(c.player.method_6047().method_31573(class_3489.field_42611) || c.player.method_6047().method_31573(class_3489.field_42612))) return;
        class_1657 best = null;
        float wantedYaw = 0, wantedPitch = 0, bestDelta = Float.MAX_VALUE;
        for (class_1657 other : c.world.method_18456()) {
            if (!Cael3stisClient.isEnemy(c.player, other) || !c.player.method_6057(other)) continue;
            if (c.player.method_5858(other) > range.value * range.value) continue;
          class_243 delta = new class_243(other.method_23317(), other.method_23318(), other.method_23321()).method_1031(0, other.method_17682() * (targetPart.is("Head") ? 0.88 : 0.58), 0).method_1020(c.player.method_33571());
            float yaw = (float) (Math.toDegrees(Math.atan2(delta.field_1350, delta.field_1352)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.atan2(delta.field_1351, Math.sqrt(delta.field_1352 * delta.field_1352 + delta.field_1350 * delta.field_1350)));
            float yawDiff = Math.abs(class_3532.method_15393(yaw - c.player.method_36454()));
            float pitchDiff = Math.abs(class_3532.method_15393(pitch - c.player.method_36455()));
            float angle = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            if (angle < bestDelta && angle <= fov.value / 2.0f) {
                bestDelta = angle;
                best = other;
                wantedYaw = yaw;
                wantedPitch = pitch;
            }
        }
        if (best == null) return;
        float factor = (strength.value / 100f) * (1.0f - (smoothness.value - 1) / 125f);
        float maxDegreesPerTick = 8.0f;
        float dy = class_3532.method_15363(class_3532.method_15393(wantedYaw - c.player.method_36454()) * factor, -maxDegreesPerTick, maxDegreesPerTick);
        float dp = class_3532.method_15363(class_3532.method_15393(wantedPitch - c.player.method_36455()) * factor, -maxDegreesPerTick, maxDegreesPerTick);
        c.player.method_36456(c.player.method_36454() + dy);
        c.player.method_36457(class_3532.method_15363(c.player.method_36455() + dp, -90f, 90f));
    }
}
