package com.caelestis.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

/** Hotbar / inventory helpers. Everything here uses normal vanilla packets (slot select, slot click). */
public final class InvUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private InvUtil() {
    }

    public static int selected() {
        return mc.player.getInventory().getSelectedSlot();
    }

    public static void select(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
    }

    public static ItemStack stack(int invIndex) {
        return mc.player.getInventory().getStack(invIndex);
    }

    /** Hotbar slot 0-8 holding a matching stack, or -1. */
    public static int findHotbar(Predicate<ItemStack> pred) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = stack(i);
            if (!s.isEmpty() && pred.test(s)) return i;
        }
        return -1;
    }

    /** Main inventory index 9-35 holding a matching stack, or -1. */
    public static int findMainInventory(Predicate<ItemStack> pred) {
        for (int i = 9; i < 36; i++) {
            ItemStack s = stack(i);
            if (!s.isEmpty() && pred.test(s)) return i;
        }
        return -1;
    }

    public static int findHotbarItem(Item item) {
        return findHotbar(s -> s.isOf(item));
    }

    public static int countItem(Item item) {
        int n = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = stack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        ItemStack off = mc.player.getOffHandStack();
        if (off.isOf(item)) n += off.getCount();
        return n;
    }

    /** Container slot id (player screen handler) for an inventory index 0-35. */
    public static int slotId(int invIndex) {
        return invIndex < 9 ? 36 + invIndex : invIndex;
    }

    /** Vanilla "number key" swap between an inventory slot and a hotbar slot. */
    public static void swapWithHotbar(int invIndex, int hotbarSlot) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotId(invIndex), hotbarSlot,
                SlotActionType.SWAP, mc.player);
    }

    /** Vanilla "F key" swap between an inventory slot and the offhand. */
    public static void swapWithOffhand(int invIndex) {
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotId(invIndex), 40,
                SlotActionType.SWAP, mc.player);
    }
}
