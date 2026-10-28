package com.caelestis.client.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;

public final class ItemUtil {
    private ItemUtil() {
    }

    public static boolean isSword(ItemStack s) {
        return !s.isEmpty() && s.isIn(ItemTags.SWORDS);
    }

    public static boolean isAxe(ItemStack s) {
        return !s.isEmpty() && s.isIn(ItemTags.AXES);
    }

    public static boolean isMace(ItemStack s) {
        return !s.isEmpty() && s.isOf(Items.MACE);
    }

    public static int enchantLevel(ItemStack stack, RegistryKey<Enchantment> key) {
        ItemEnchantmentsComponent ench = stack.getEnchantments();
        for (RegistryEntry<Enchantment> e : ench.getEnchantments()) {
            if (e.matchesKey(key)) return ench.getLevel(e);
        }
        return 0;
    }

    /**
     * True only for a SPLASH potion whose actual effect list is non-empty and consists exclusively of
     * Instant Health (Healing I / II). Identification is by real potion contents, never by name.
     * Lingering and drinkable potions are rejected because the item must be Items.SPLASH_POTION.
     */
    public static boolean isHealingSplash(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.SPLASH_POTION)) return false;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        boolean any = false;
        for (StatusEffectInstance inst : contents.getEffects()) {
            if (!inst.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) return false;
            any = true;
        }
        return any;
    }

    /** Amplifier of the Instant Health effect (0 = Healing I, 1 = Healing II). -1 if not a healing splash. */
    public static int healingAmplifier(ItemStack stack) {
        if (!isHealingSplash(stack)) return -1;
        int best = 0;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        for (StatusEffectInstance inst : contents.getEffects()) {
            best = Math.max(best, inst.getAmplifier());
        }
        return best;
    }
}
