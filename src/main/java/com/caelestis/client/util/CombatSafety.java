package com.caelestis.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Single source of truth for "is this explosion safe for me?" so Crystal Aura, Anchor Aura, Anti Crystal and
 * Auto Totem never disagree about what counts as dangerous.
 */
public final class CombatSafety {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private CombatSafety() {
    }

    public static float effectiveHealth() {
        ClientPlayerEntity p = mc.player;
        return p == null ? 0f : p.getHealth() + p.getAbsorptionAmount();
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
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return 0f;
        float worst = 0f;
        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                p.getBoundingBox().expand(range), e -> e.isAlive())) {
            Vec3d pos = new Vec3d(c.getX(), c.getY(), c.getZ());
            worst = Math.max(worst, DamageUtil.crystalDamage(p, pos));
        }
        return worst;
    }
}
