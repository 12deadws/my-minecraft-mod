package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.InvUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Tops up combat items in the hotbar from the main inventory using the vanilla hotbar-swap click. One move per
 * action, only when a stack is below its minimum and the source stack is actually bigger, never displaces a
 * different item, and stays out of the way of survival / combat modules.
 */
public class AutoRefill extends Module {
    private final NumberSetting delay = add(new NumberSetting("Refill Delay", "Minimum time between two inventory moves.", 150, 50, 1000, 10, " ms"));
    private final NumberSetting pauseBelow = add(new NumberSetting("Pause Below HP", "Pause refilling while your health is at or below this (critical combat).", 8, 0, 20, 1, " HP"));

    private static final class Entry {
        final String label;
        final Item[] items;
        final BoolSetting enabled;
        final NumberSetting min;
        final NumberSetting slot;

        Entry(String label, Item[] items, BoolSetting enabled, NumberSetting min, NumberSetting slot) {
            this.label = label;
            this.items = items;
            this.enabled = enabled;
            this.min = min;
            this.slot = slot;
        }

        boolean matches(ItemStack s) {
            for (Item i : items) if (s.isOf(i)) return true;
            return false;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private long lastMove = 0;

    public AutoRefill() {
        super("Auto Refill", "Refills crystals, obsidian, gapples, totems, anchors and glowstone in your hotbar.", Category.PLAYER, 100);
        addEntry("End Crystals", new Item[]{Items.END_CRYSTAL}, 16, 0);
        addEntry("Obsidian", new Item[]{Items.OBSIDIAN}, 16, 0);
        addEntry("Golden Apples", new Item[]{Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE}, 8, 0);
        addEntry("Totems", new Item[]{Items.TOTEM_OF_UNDYING}, 1, 0);
        addEntry("Respawn Anchors", new Item[]{Items.RESPAWN_ANCHOR}, 4, 0);
        addEntry("Glowstone", new Item[]{Items.GLOWSTONE}, 8, 0);
    }

    private void addEntry(String label, Item[] items, int defMin, int defSlot) {
        BoolSetting en = add(new BoolSetting(label, "Refill " + label.toLowerCase() + " in the hotbar.", true));
        NumberSetting min = add(new NumberSetting(label + " Min", "Refill when the hotbar stack drops below this count.", defMin, 1, 64, 1));
        NumberSetting slot = add(new NumberSetting(label + " Slot", "Hotbar slot (1-9) to keep this item in. 0 = automatic.", defSlot, 0, 9, 1));
        entries.add(new Entry(label, items, en, min, slot));
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastMove < (long) delay.get()) return;
        if (p.getHealth() <= pauseBelow.getInt()) return;

        // Stay out of the way of critical inventory / hotbar / attack actions by other modules.
        if (ActionManager.ticksSinceUse(Channel.INVENTORY) < 8) return;
        if (ActionManager.ticksSinceUse(Channel.HOTBAR) < 3) return;
        if (!ActionManager.isFree(Channel.INVENTORY, this)) return;

        for (Entry e : entries) {
            if (!e.enabled.get()) continue;

            int target = targetSlot(e);
            if (target < 0) continue;

            ItemStack inSlot = InvUtil.stack(target);
            int cur = (!inSlot.isEmpty() && e.matches(inSlot)) ? inSlot.getCount() : 0;
            if (cur >= e.min.getInt()) continue;
            if (!inSlot.isEmpty() && !e.matches(inSlot)) continue; // never displace a different item

            int src = InvUtil.findMainInventory(e::matches);
            if (src < 0) continue;
            if (InvUtil.stack(src).getCount() <= cur) continue; // would not improve anything

            if (!ActionManager.claim(Channel.INVENTORY, this)) return;
            InvUtil.swapWithHotbar(src, target);
            lastMove = System.currentTimeMillis();
            return; // one move per action
        }
    }

    /** Hotbar slot 0-8 to refill, or -1 if there is nowhere to put it. */
    private int targetSlot(Entry e) {
        int cfg = e.slot.getInt();
        if (cfg > 0) return cfg - 1;
        int existing = InvUtil.findHotbar(e::matches);
        if (existing >= 0) return existing;
        for (int i = 0; i < 9; i++) {
            if (InvUtil.stack(i).isEmpty()) return i;
        }
        return -1;
    }

    @Override
    public void onWorldChange() {
        lastMove = 0;
    }
}
