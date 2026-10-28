package com.caelestis.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Projects world positions to scaled-GUI screen coordinates so NameTags and Hole ESP can be drawn as 2D
 * overlays. This deliberately avoids the world-render pipeline (which changes every few Minecraft versions and
 * is where Sodium integration usually breaks) and works identically with or without Sodium.
 *
 * Limitations: first-person only, and uses the FOV from the options screen (so sprint / speed FOV effects
 * cause a small misalignment).
 */
public final class Projector {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static Vec3d eye = Vec3d.ZERO;
    private static Vec3d fwd = Vec3d.ZERO, right = Vec3d.ZERO, up = Vec3d.ZERO;
    private static double tanHalfFov = 1.0, aspect = 1.0;
    private static int width = 1, height = 1;
    private static boolean valid = false;

    private Projector() {
    }

    /** Call once per frame before project(). Returns false if projection is not usable (third person). */
    public static boolean begin(float tickDelta) {
        valid = false;
        if (mc.player == null || mc.world == null) return false;
        if (!mc.options.getPerspective().isFirstPerson()) return false;

        Vec3d pos = mc.player.getLerpedPos(tickDelta);
        eye = pos.add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);

        double yaw = Math.toRadians(mc.player.getYaw(tickDelta));
        double pitch = Math.toRadians(mc.player.getPitch(tickDelta));
        double cp = Math.cos(pitch), sp = Math.sin(pitch), cy = Math.cos(yaw), sy = Math.sin(yaw);
        fwd = new Vec3d(-sy * cp, -sp, cy * cp);
        right = new Vec3d(-cy, 0, -sy);
        up = right.crossProduct(fwd);

        double fov = mc.options.getFov().getValue();
        tanHalfFov = Math.tan(Math.toRadians(fov) / 2.0);
        width = mc.getWindow().getScaledWidth();
        height = mc.getWindow().getScaledHeight();
        aspect = (double) width / (double) height;
        valid = true;
        return true;
    }

    /** Returns {screenX, screenY} or null if the point is behind the camera. */
    public static double[] project(Vec3d world) {
        if (!valid) return null;
        Vec3d d = world.subtract(eye);
        double z = d.dotProduct(fwd);
        if (z <= 0.05) return null;
        double x = d.dotProduct(right);
        double y = d.dotProduct(up);
        double ndcX = (x / z) / (tanHalfFov * aspect);
        double ndcY = (y / z) / tanHalfFov;
        return new double[]{width / 2.0 * (1.0 + ndcX), height / 2.0 * (1.0 - ndcY)};
    }
}
