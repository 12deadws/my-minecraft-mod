package com.caelestis.client.util;

import net.minecraft.class_243;
import net.minecraft.class_310;

/**
 * Projects world positions to scaled-GUI screen coordinates so NameTags and Hole ESP can be drawn as 2D
 * overlays. This deliberately avoids the world-render pipeline (which changes every few Minecraft versions and
 * is where Sodium integration usually breaks) and works identically with or without Sodium.
 *
 * Limitations: first-person only, and uses the FOV from the options screen (so sprint / speed FOV effects
 * cause a small misalignment).
 */
public final class Projector {
    private static final class_310 mc = class_310.method_1551();

    private static class_243 eye = class_243.field_1353;
    private static class_243 fwd = class_243.field_1353, right = class_243.field_1353, up = class_243.field_1353;
    private static double tanHalfFov = 1.0, aspect = 1.0;
    private static int width = 1, height = 1;
    private static boolean valid = false;

    private Projector() {
    }

    /** Call once per frame before project(). Returns false if projection is not usable (third person). */
    public static boolean begin(float tickDelta) {
        valid = false;
        if (mc.field_1724 == null || mc.field_1687 == null) return false;
        if (!mc.field_1690.method_31044().method_31034()) return false;

        class_243 pos = mc.field_1724.method_30950(tickDelta);
        eye = pos.method_1031(0, mc.field_1724.method_18381(mc.field_1724.method_18376()), 0);

        double yaw = Math.toRadians(mc.field_1724.method_5705(tickDelta));
        double pitch = Math.toRadians(mc.field_1724.method_5695(tickDelta));
        double cp = Math.cos(pitch), sp = Math.sin(pitch), cy = Math.cos(yaw), sy = Math.sin(yaw);
        fwd = new class_243(-sy * cp, -sp, cy * cp);
        right = new class_243(-cy, 0, -sy);
        up = right.method_1036(fwd);

        double fov = mc.field_1690.method_41808().method_41753();
        tanHalfFov = Math.tan(Math.toRadians(fov) / 2.0);
        width = mc.method_22683().method_4486();
        height = mc.method_22683().method_4502();
        aspect = (double) width / (double) height;
        valid = true;
        return true;
    }

    /** Returns {screenX, screenY} or null if the point is behind the camera. */
    public static double[] project(class_243 world) {
        if (!valid) return null;
        class_243 d = world.method_1020(eye);
        double z = d.method_1026(fwd);
        if (z <= 0.05) return null;
        double x = d.method_1026(right);
        double y = d.method_1026(up);
        double ndcX = (x / z) / (tanHalfFov * aspect);
        double ndcY = (y / z) / tanHalfFov;
        return new double[]{width / 2.0 * (1.0 + ndcX), height / 2.0 * (1.0 - ndcY)};
    }
}
