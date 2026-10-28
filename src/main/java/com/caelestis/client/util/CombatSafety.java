package com.caelestis.client.util;

import net.minecraft.class_1511;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_746;

/**
 * Single source of truth for "is this explosion safe for me?" so Crystal Aura, Anchor Aura, Anti Crystal and
 * Auto Totem never disagree about what counts as dangerous.
 */
public final class CombatSafety {
    private static final class_310 mc = class_310.method_1551();

    private CombatSafety() {
    }

    public static float effectiveHealth() {
        class_746 p = mc.field_1724;
        return p == null ? 0f : p.method_6032() + p.method_6067();
    }

    public static boolean isLethal(float selfDamage) {
        return selfDamage >= effectiveHealth() - 0.5f;
    }

    public static boolean isSelfSafe(float selfDamage, double maxSelf, boolean antiSuicide) {
        if (selfDamage > maxSelf) return false;
        return !antiSuicide || !isLethal(selfDamage);
    }

    /** Highest self-damage among all nearby crystals; used by smart Auto Totem. */
    public static float worstCrystalThreat(double range) {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1687 == null) return 0f;
        float worst = 0f;
        for (class_1511 c : mc.field_1687.method_8390(class_1511.class,
                p.method_5829().method_1014(range), e -> e.method_5805())) {
            class_243 pos = new class_243(c.method_23317(), c.method_23318(), c.method_23321());
            worst = Math.max(worst, DamageUtil.crystalDamage(p, pos));
        }
        return worst;
    }
}
