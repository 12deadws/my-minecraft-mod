package com.caelestis.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Explosion damage estimate following the vanilla formula (distance falloff, sampled exposure, difficulty,
 * armor + toughness, Resistance, Protection / Blast Protection). Crystal Aura, Anti Crystal, Anchor Aura and
 * Auto Totem (smart mode) all use this single implementation so their safety decisions agree.
 *
 * Exposure uses a coarser sampling grid than vanilla to keep the per-tick cost low, so results are an
 * estimate (typically within about a heart), not an exact prediction.
 */
public final class DamageUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final float CRYSTAL_POWER = 6.0f;
    public static final float ANCHOR_POWER = 5.0f;

    private DamageUtil() {
    }

    public static float crystalDamage(LivingEntity target, Vec3d crystalPos) {
        return explosionDamage(target, crystalPos, CRYSTAL_POWER, null);
    }

    /** @param ignoreBlock a block that will be removed before the blast (the anchor itself), or null. */
    public static float explosionDamage(LivingEntity target, Vec3d center, float power, BlockPos ignoreBlock) {
        if (mc.world == null) return 0f;
        double q = power * 2.0;
        double dist = Math.sqrt(target.squaredDistanceTo(center)) / q;
        if (dist > 1.0) return 0f;

        double exposure = exposure(center, target, ignoreBlock);
        double impact = (1.0 - dist) * exposure;
        float dmg = (float) ((impact * impact + impact) / 2.0 * 7.0 * q + 1.0);

        switch (mc.world.getDifficulty()) {
            case PEACEFUL -> {
                return 0f;
            }
            case EASY -> dmg = Math.min(dmg / 2f + 1f, dmg);
            case HARD -> dmg *= 1.5f;
            default -> {
            }
        }
        return Math.max(0f, reduce(target, dmg));
    }

    private static float reduce(LivingEntity e, float dmg) {
        // Armor + toughness
        float armor = e.getArmor();
        float toughness = (float) e.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
        float f = 2.0f + toughness / 4.0f;
        float g = MathHelper.clamp(armor - dmg / f, armor * 0.2f, 20.0f);
        dmg *= 1.0f - g / 25.0f;

        // Resistance
        StatusEffectInstance res = e.getStatusEffect(StatusEffects.RESISTANCE);
        if (res != null) {
            dmg *= Math.max(0f, 1.0f - 0.2f * (res.getAmplifier() + 1));
        }

        // Protection (1 EPF / level) and Blast Protection (2 EPF / level), capped at 20
        int epf = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = e.getEquippedStack(slot);
            if (s.isEmpty()) continue;
            ItemEnchantmentsComponent ench = s.getEnchantments();
            for (RegistryEntry<Enchantment> en : ench.getEnchantments()) {
                int lvl = ench.getLevel(en);
                if (en.matchesKey(Enchantments.BLAST_PROTECTION)) epf += lvl * 2;
                else if (en.matchesKey(Enchantments.PROTECTION)) epf += lvl;
            }
        }
        epf = Math.min(epf, 20);
        dmg *= 1.0f - epf / 25.0f;
        return dmg;
    }

    private static double exposure(Vec3d src, LivingEntity e, BlockPos ignore) {
        Box b = e.getBoundingBox();
        // Coarse grid (1.8x vanilla step) to keep cost low.
        double dx = 1.0 / ((b.maxX - b.minX) * 2.0 + 1.0) * 1.8;
        double dy = 1.0 / ((b.maxY - b.minY) * 2.0 + 1.0) * 1.8;
        double dz = 1.0 / ((b.maxZ - b.minZ) * 2.0 + 1.0) * 1.8;
        if (dx <= 0 || dy <= 0 || dz <= 0) return 0.0;
        double ox = (1.0 - Math.floor(1.0 / dx) * dx) / 2.0;
        double oz = (1.0 - Math.floor(1.0 / dz) * dz) / 2.0;

        int hit = 0, total = 0;
        for (double x = 0.0; x <= 1.0; x += dx) {
            for (double y = 0.0; y <= 1.0; y += dy) {
                for (double z = 0.0; z <= 1.0; z += dz) {
                    Vec3d pt = new Vec3d(
                            MathHelper.lerp(x, b.minX, b.maxX) + ox,
                            MathHelper.lerp(y, b.minY, b.maxY),
                            MathHelper.lerp(z, b.minZ, b.maxZ) + oz);
                    if (!blocked(pt, src, e, ignore)) hit++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) hit / (double) total;
    }

    private static boolean blocked(Vec3d from, Vec3d to, LivingEntity e, BlockPos ignore) {
        BlockHitResult r = mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, e));
        if (r.getType() == HitResult.Type.MISS) return false;
        // First thing hit is the block that will be removed by the blast -> nothing is in the way.
        return ignore == null || !r.getBlockPos().equals(ignore);
    }
}
