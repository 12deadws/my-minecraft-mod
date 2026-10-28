package com.caelestis.client.util;

import java.util.function.Predicate;
import net.minecraft.class_1713;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_310;

/** Hotbar / inventory helpers. Everything here uses normal vanilla packets (slot select, slot click). */
public final class InvUtil {
    private static final class_310 mc = class_310.method_1551();

    private InvUtil() {
    }

    public static int selected() {
        return mc.field_1724.method_31548().method_67532();
    }

    public static void select(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.field_1724.method_31548().method_61496(slot);
    }

    public static class_1799 stack(int invIndex) {
        return mc.field_1724.method_31548().method_5438(invIndex);
    }

    /** Hotbar slot 0-8 holding a matching stack, or -1. */
    public static int findHotbar(Predicate<class_1799> pred) {
        for (int i = 0; i < 9; i++) {
            class_1799 s = stack(i);
            if (!s.method_7960() && pred.test(s)) return i;
        }
        return -1;
    }

    /** Main inventory index 9-35 holding a matching stack, or -1. */
    public static int findMainInventory(Predicate<class_1799> pred) {
        for (int i = 9; i < 36; i++) {
            class_1799 s = stack(i);
            if (!s.method_7960() && pred.test(s)) return i;
        }
        return -1;
    }

    public static int findHotbarItem(class_1792 item) {
        return findHotbar(s -> s.method_31574(item));
    }

    public static int countItem(class_1792 item) {
        int n = 0;
        for (int i = 0; i < 36; i++) {
            class_1799 s = stack(i);
            if (s.method_31574(item)) n += s.method_7947();
        }
        class_1799 off = mc.field_1724.method_6079();
        if (off.method_31574(item)) n += off.method_7947();
        return n;
    }

    /** Container slot id (player screen handler) for an inventory index 0-35. */
    public static int slotId(int invIndex) {
        return invIndex < 9 ? 36 + invIndex : invIndex;
    }

    /** Vanilla "number key" swap between an inventory slot and a hotbar slot. */
    public static void swapWithHotbar(int invIndex, int hotbarSlot) {
        mc.field_1761.method_2906(mc.field_1724.field_7498.field_7763, slotId(invIndex), hotbarSlot,
                class_1713.field_7791, mc.field_1724);
    }

    /** Vanilla "F key" swap between an inventory slot and the offhand. */
    public static void swapWithOffhand(int invIndex) {
        mc.field_1761.method_2906(mc.field_1724.field_7498.field_7763, slotId(invIndex), 40,
                class_1713.field_7791, mc.field_1724);
    }
}
