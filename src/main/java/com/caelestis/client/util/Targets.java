package com.caelestis.client.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_746;

/** Shared target selection helpers so every module agrees on who is an enemy. */
public final class Targets {
    private static final class_310 mc = class_310.method_1551();

    private static class_1657 preferred;
    private static long preferredAt;

    private Targets() {
    }

    /** TriggerBot tells Soft Aim which player it is hitting so both stay on the same target. */
    public static void prefer(class_1657 p) {
        preferred = p;
        preferredAt = System.currentTimeMillis();
    }

    public static class_1657 getPreferred() {
        if (preferred != null && System.currentTimeMillis() - preferredAt < 1500 && isValidEnemy(preferred)) {
            return preferred;
        }
        return null;
    }

    public static void reset() {
        preferred = null;
    }

    public static boolean isValidEnemy(class_1657 p) {
        class_746 me = mc.field_1724;
        if (me == null || p == null || p == me) return false;
        if (!p.method_5805() || p.method_29504() || p.method_7325() || p.method_6032() <= 0f) return false;
        if (Friends.isFriend(p.method_5477().getString())) return false;
        return !me.method_5722(p);
    }

    public static List<class_1657> enemies(double range) {
        List<class_1657> out = new ArrayList<>();
        if (mc.field_1687 == null || mc.field_1724 == null) return out;
        for (class_1657 p : mc.field_1687.method_18456()) {
            if (isValidEnemy(p) && mc.field_1724.method_5739(p) <= range) out.add(p);
        }
        return out;
    }

    public static class_1657 nearestEnemy(double range) {
        class_1657 best = null;
        double bd = Double.MAX_VALUE;
        for (class_1657 p : enemies(range)) {
            double d = mc.field_1724.method_5858(p);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    /** Unobstructed line from the player's eyes to the given point (blocks only). */
    public static boolean visible(class_243 to) {
        if (mc.field_1724 == null || mc.field_1687 == null) return false;
        class_243 eye = mc.field_1724.method_33571();
        class_3965 r = mc.field_1687.method_17742(new class_3959(eye, to, class_3959.class_3960.field_17558,
                class_3959.class_242.field_1348, mc.field_1724));
        return r.method_17783() == class_239.class_240.field_1333;
    }

    public static boolean canSee(class_1297 e) {
        if (mc.field_1724 == null) return false;
        return mc.field_1724.method_6057(e);
    }

    /** Distance from the eyes to the closest point of an entity's hitbox (what the server validates). */
    public static double reachTo(class_1297 e) {
        class_243 eye = mc.field_1724.method_33571();
        class_238 b = e.method_5829();
        class_243 closest = new class_243(
                class_3532.method_15350(eye.field_1352, b.field_1323, b.field_1320),
                class_3532.method_15350(eye.field_1351, b.field_1322, b.field_1325),
                class_3532.method_15350(eye.field_1350, b.field_1321, b.field_1324));
        return eye.method_1022(closest);
    }

    /** Vanilla entity interaction range (3.0 by default) capped by a user setting. */
    public static double entityReach(double userRange) {
        return Math.min(userRange, mc.field_1724.method_55755());
    }

    public static double blockReach(double userRange) {
        return Math.min(userRange, mc.field_1724.method_55754());
    }
}
