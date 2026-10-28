package com.caelestis.client.util;

import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1893;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_5134;
import net.minecraft.class_6880;
import net.minecraft.class_9304;

/**
 * Explosion damage estimate following the vanilla formula (distance falloff, sampled exposure, difficulty,
 * armor + toughness, Resistance, Protection / Blast Protection). Crystal Aura, Anti Crystal, Anchor Aura and
 * Auto Totem (smart mode) all use this single implementation so their safety decisions agree.
 *
 * Exposure uses a coarser sampling grid than vanilla to keep the per-tick cost low, so results are an
 * estimate (typically within about a heart), not an exact prediction.
 */
public final class DamageUtil {
    private static final class_310 mc = class_310.method_1551();

    public static final float CRYSTAL_POWER = 6.0f;
    public static final float ANCHOR_POWER = 5.0f;

    private DamageUtil() {
    }

    public static float crystalDamage(class_1309 target, class_243 crystalPos) {
        return explosionDamage(target, crystalPos, CRYSTAL_POWER, null);
    }

    /** @param ignoreBlock a block that will be removed before the blast (the anchor itself), or null. */
    public static float explosionDamage(class_1309 target, class_243 center, float power, class_2338 ignoreBlock) {
        if (mc.field_1687 == null) return 0f;
        double q = power * 2.0;
        double dist = Math.sqrt(target.method_5707(center)) / q;
        if (dist > 1.0) return 0f;

        double exposure = exposure(center, target, ignoreBlock);
        double impact = (1.0 - dist) * exposure;
        float dmg = (float) ((impact * impact + impact) / 2.0 * 7.0 * q + 1.0);

        switch (mc.field_1687.method_8407()) {
            case field_5801 -> {
                return 0f;
            }
            case field_5805 -> dmg = Math.min(dmg / 2f + 1f, dmg);
            case field_5807 -> dmg *= 1.5f;
            default -> {
            }
        }
        return Math.max(0f, reduce(target, dmg));
    }

    private static float reduce(class_1309 e, float dmg) {
        // Armor + toughness
        float armor = e.method_6096();
        float toughness = (float) e.method_45325(class_5134.field_23725);
        float f = 2.0f + toughness / 4.0f;
        float g = class_3532.method_15363(armor - dmg / f, armor * 0.2f, 20.0f);
        dmg *= 1.0f - g / 25.0f;

        // Resistance
        class_1293 res = e.method_6112(class_1294.field_5907);
        if (res != null) {
            dmg *= Math.max(0f, 1.0f - 0.2f * (res.method_5578() + 1));
        }

        // Protection (1 EPF / level) and Blast Protection (2 EPF / level), capped at 20
        int epf = 0;
        for (class_1304 slot : new class_1304[]{class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166}) {
            class_1799 s = e.method_6118(slot);
            if (s.method_7960()) continue;
            class_9304 ench = s.method_58657();
            for (class_6880<class_1887> en : ench.method_57534()) {
                int lvl = ench.method_57536(en);
                if (en.method_40225(class_1893.field_9107)) epf += lvl * 2;
                else if (en.method_40225(class_1893.field_9111)) epf += lvl;
            }
        }
        epf = Math.min(epf, 20);
        dmg *= 1.0f - epf / 25.0f;
        return dmg;
    }

    private static double exposure(class_243 src, class_1309 e, class_2338 ignore) {
        class_238 b = e.method_5829();
        // Coarse grid (1.8x vanilla step) to keep cost low.
        double dx = 1.0 / ((b.field_1320 - b.field_1323) * 2.0 + 1.0) * 1.8;
        double dy = 1.0 / ((b.field_1325 - b.field_1322) * 2.0 + 1.0) * 1.8;
        double dz = 1.0 / ((b.field_1324 - b.field_1321) * 2.0 + 1.0) * 1.8;
        if (dx <= 0 || dy <= 0 || dz <= 0) return 0.0;
        double ox = (1.0 - Math.floor(1.0 / dx) * dx) / 2.0;
        double oz = (1.0 - Math.floor(1.0 / dz) * dz) / 2.0;

        int hit = 0, total = 0;
        for (double x = 0.0; x <= 1.0; x += dx) {
            for (double y = 0.0; y <= 1.0; y += dy) {
                for (double z = 0.0; z <= 1.0; z += dz) {
                    class_243 pt = new class_243(
                            class_3532.method_16436(x, b.field_1323, b.field_1320) + ox,
                            class_3532.method_16436(y, b.field_1322, b.field_1325),
                            class_3532.method_16436(z, b.field_1321, b.field_1324) + oz);
                    if (!blocked(pt, src, e, ignore)) hit++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hit / (double) total;
    }

    private static boolean blocked(class_243 from, class_243 to, class_1309 e, class_2338 ignore) {
        class_3965 r = mc.field_1687.method_17742(new class_3959(from, to, class_3959.class_3960.field_17558,
                class_3959.class_242.field_1348, e));
        if (r.method_17783() == class_239.class_240.field_1333) return false;
        // First thing hit is the block that will be removed by the blast -> nothing is in the way.
        return ignore == null || !r.method_17777().equals(ignore);
    }
}
