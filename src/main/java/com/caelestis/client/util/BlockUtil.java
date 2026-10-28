package com.caelestis.client.util;

import java.util.function.Predicate;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_2237;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3726;
import net.minecraft.class_3965;

/** Block placement helpers that follow normal vanilla placement rules and reach. */
public final class BlockUtil {
    private static final class_310 mc = class_310.method_1551();

    private BlockUtil() {
    }

    public static boolean isObsidianOrBedrock(class_2680 s) {
        return s.method_27852(class_2246.field_10540) || s.method_27852(class_2246.field_9987);
    }

    /** A block face that can be right-clicked to place against without triggering a GUI/interaction. */
    private static boolean isSafeSupport(class_2338 pos) {
        class_2680 s = mc.field_1687.method_8320(pos);
        if (s.method_26215() || s.method_45474() || !s.method_26227().method_15769()) return false;
        class_2248 b = s.method_26204();
        if (b instanceof class_2237) return false;
        if (s.method_27852(class_2246.field_9980) || s.method_27852(class_2246.field_10179) || s.method_27852(class_2246.field_10183)
                || s.method_27852(class_2246.field_23152) || s.method_27852(class_2246.field_10375) || s.method_27852(class_2246.field_16336)
                || s.method_27852(class_2246.field_16331) || s.method_27852(class_2246.field_16329) || s.method_27852(class_2246.field_10083)
                || s.method_27852(class_2246.field_16335) || s.method_27852(class_2246.field_16337) || s.method_27852(class_2246.field_16330)) return false;
        return s.method_26234(mc.field_1687, pos);
    }

    /** Can an obsidian block legally be placed at pos (replaceable, no entity in the way)? */
    public static boolean canPlaceBlockAt(class_2338 pos) {
        if (mc.field_1687 == null) return false;
        if (!mc.field_1687.method_8320(pos).method_45474()) return false;
        return mc.field_1687.method_8628(class_2246.field_10540.method_9564(), pos, class_3726.method_16194());
    }

    /** Finds an adjacent support block and returns the click that places a block at pos, or null. */
    public static class_3965 findPlacement(class_2338 pos, double maxReach) {
        if (mc.field_1724 == null || mc.field_1687 == null) return null;
        class_243 eye = mc.field_1724.method_33571();
        class_3965 best = null;
        double bestDist = Double.MAX_VALUE;
        for (class_2350 dir : class_2350.values()) {
            class_2338 support = pos.method_10093(dir);
            if (!isSafeSupport(support)) continue;
            class_2350 face = dir.method_10153();
            class_243 hit = class_243.method_24953(support).method_1031(face.method_10148() * 0.5, face.method_10164() * 0.5, face.method_10165() * 0.5);
            double d = eye.method_1022(hit);
            if (d > maxReach || d >= bestDist) continue;
            bestDist = d;
            best = new class_3965(hit, face, support, false);
        }
        return best;
    }

    /**
     * Places an item block at pos using a hotbar item matched by pred. Temporarily switches slot if allowed and
     * restores it in the same tick. Returns true if a placement packet was sent.
     */
    public static boolean placeBlock(class_2338 pos, Predicate<class_1799> itemPred, boolean autoSwitch, boolean restore) {
        if (mc.field_1724 == null || mc.field_1761 == null) return false;
        if (!canPlaceBlockAt(pos)) return false;
        class_3965 hit = findPlacement(pos, mc.field_1724.method_55754());
        if (hit == null) return false;

        int prev = InvUtil.selected();
        int slot;
        if (itemPred.test(mc.field_1724.method_6047())) {
            slot = prev;
        } else if (autoSwitch) {
            slot = InvUtil.findHotbar(itemPred);
            if (slot < 0) return false;
        } else {
            return false;
        }
        if (slot != prev) InvUtil.select(slot);
        mc.field_1761.method_2896(mc.field_1724, class_1268.field_5808, hit);
        mc.field_1724.method_6104(class_1268.field_5808);
        if (slot != prev && restore) InvUtil.select(prev);
        return true;
    }

    public static boolean isInRange(class_2338 pos, double range) {
        return mc.field_1724.method_33571().method_1022(class_243.method_24953(pos)) <= range;
    }
}
