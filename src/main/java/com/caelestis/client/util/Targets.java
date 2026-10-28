package com.caelestis.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/** Shared target selection helpers so every module agrees on who is an enemy. */
public final class Targets {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static PlayerEntity preferred;
    private static long preferredAt;

    private Targets() {
    }

    /** TriggerBot tells Soft Aim which player it is hitting so both stay on the same target. */
    public static void prefer(PlayerEntity p) {
        preferred = p;
        preferredAt = System.currentTimeMillis();
    }

    public static PlayerEntity getPreferred() {
        if (preferred != null && System.currentTimeMillis() - preferredAt < 1500 && isValidEnemy(preferred)) {
            return preferred;
        }
        return null;
    }

    public static void reset() {
        preferred = null;
    }

    public static boolean isValidEnemy(PlayerEntity p) {
        ClientPlayerEntity me = mc.player;
        if (me == null || p == null || p == me) return false;
        if (!p.isAlive() || p.isDead() || p.isSpectator() || p.getHealth() <= 0f) return false;
        if (Friends.isFriend(p.getName().getString())) return false;
        return !me.isTeammate(p);
    }

    public static List<PlayerEntity> enemies(double range) {
        List<PlayerEntity> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (isValidEnemy(p) && mc.player.distanceTo(p) <= range) out.add(p);
        }
        return out;
    }

    public static PlayerEntity nearestEnemy(double range) {
        PlayerEntity best = null;
        double bd = Double.MAX_VALUE;
        for (PlayerEntity p : enemies(range)) {
            double d = mc.player.squaredDistanceTo(p);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    /** Unobstructed line from the player's eyes to the given point (blocks only). */
    public static boolean visible(Vec3d to) {
        if (mc.player == null || mc.world == null) return false;
        Vec3d eye = mc.player.getEyePos();
        BlockHitResult r = mc.world.raycast(new RaycastContext(eye, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        return r.getType() == HitResult.Type.MISS;
    }

    public static boolean canSee(Entity e) {
        if (mc.player == null) return false;
        return mc.player.canSee(e);
    }

    /** Distance from the eyes to the closest point of an entity's hitbox (what the server validates). */
    public static double reachTo(Entity e) {
        Vec3d eye = mc.player.getEyePos();
        Box b = e.getBoundingBox();
        Vec3d closest = new Vec3d(
                MathHelper.clamp(eye.x, b.minX, b.maxX),
                MathHelper.clamp(eye.y, b.minY, b.maxY),
                MathHelper.clamp(eye.z, b.minZ, b.maxZ));
        return eye.distanceTo(closest);
    }

    /** Vanilla entity interaction range (3.0 by default) capped by a user setting. */
    public static double entityReach(double userRange) {
        return Math.min(userRange, mc.player.getEntityInteractionRange());
    }

    public static double blockReach(double userRange) {
        return Math.min(userRange, mc.player.getBlockInteractionRange());
    }
}
