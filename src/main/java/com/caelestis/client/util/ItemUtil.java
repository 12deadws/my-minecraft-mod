package com.caelestis.client.util;

import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1844;
import net.minecraft.class_1887;
import net.minecraft.class_3489;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_9304;
import net.minecraft.class_9334;

public final class ItemUtil {
    private ItemUtil() {
    }

    public static boolean isSword(class_1799 s) {
        return !s.method_7960() && s.method_31573(class_3489.field_42611);
    }

    public static boolean isAxe(class_1799 s) {
        return !s.method_7960() && s.method_31573(class_3489.field_42612);
    }

    public static boolean isMace(class_1799 s) {
        return !s.method_7960() && s.method_31574(class_1802.field_49814);
    }

    public static int enchantLevel(class_1799 stack, class_5321<class_1887> key) {
        class_9304 ench = stack.method_58657();
        for (class_6880<class_1887> e : ench.method_57534()) {
            if (e.method_40225(key)) return ench.method_57536(e);
        }
        return 0;
    }

    /**
     * True only for a SPLASH potion whose actual effect list is non-empty and consists exclusively of
     * Instant Health (Healing I / II). Identification is by real potion contents, never by name.
     * Lingering and drinkable potions are rejected because the item must be Items.SPLASH_POTION.
     */
    public static boolean isHealingSplash(class_1799 stack) {
        if (stack == null || stack.method_7960() || !stack.method_31574(class_1802.field_8436)) return false;
        class_1844 contents = stack.method_58694(class_9334.field_49651);
        if (contents == null) return false;
        boolean any = false;
        for (class_1293 inst : contents.method_57397()) {
            if (!inst.method_5579().equals(class_1294.field_5915)) return false;
            any = true;
        }
        return any;
    }

    /** Amplifier of the Instant Health effect (0 = Healing I, 1 = Healing II). -1 if not a healing splash. */
    public static int healingAmplifier(class_1799 stack) {
        if (!isHealingSplash(stack)) return -1;
        int best = 0;
        class_1844 contents = stack.method_58694(class_9334.field_49651);
        for (class_1293 inst : contents.method_57397()) {
            best = Math.max(best, inst.method_5578());
        }
        return best;
    }
}
