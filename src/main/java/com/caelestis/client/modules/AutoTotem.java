package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.CombatSafety;
import com.caelestis.client.util.InvUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Keeps a Totem of Undying in the offhand using the normal inventory swap (the same packet as pressing F over a
 * slot). Highest-priority module: it ticks first and claims the inventory channel.
 */
public class AutoTotem extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Health Threshold", "Equip a totem when health is at or below this many HP.", 10, 1, 20, 1, " HP"));
    private final BoolSetting smart = add(new BoolSetting("Smart Mode", "Also equip when a nearby crystal or a long fall could kill you.", true));
    private final BoolSetting always = add(new BoolSetting("Always Totem", "Keep a totem in the offhand permanently.", false));
    private final NumberSetting refillDelay = add(new NumberSetting("Refill Delay", "Delay before re-equipping after a totem pops or the offhand empties.", 100, 0, 1000, 10, " ms"));
    private final BoolSetting restore = add(new BoolSetting("Restore Previous Offhand Item", "Put your previous offhand item back once you are safe (not used with Always Totem).", true));

    private long needSince = 0;
    private ItemStack savedOffhand = ItemStack.EMPTY;
    private boolean hadTotem = false;

    public AutoTotem() {
        super("Auto Totem", "Moves a Totem of Undying into your offhand when needed and refills it after a pop.", Category.PLAYER, 0);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        ItemStack off = p.getOffHandStack();
        boolean offIsTotem = off.isOf(Items.TOTEM_OF_UNDYING);
        boolean need = always.get() || p.getHealth() <= threshold.getInt() || (smart.get() && dangerPredicted(p));

        if (need && !offIsTotem) {
            long now = System.currentTimeMillis();
            if (needSince == 0) needSince = now;
            // First equip is instant; the delay only applies when refilling right after a totem was consumed.
            long wait = hadTotem ? (long) refillDelay.get() : 0L;
            if (now - needSince < wait) return;

            int src = findTotem(p);
            if (src < 0) return;
            if (!ActionManager.claim(Channel.INVENTORY, this)) return;

            if (!off.isEmpty()) savedOffhand = off.copy();
            InvUtil.swapWithOffhand(src);
            needSince = 0;
            hadTotem = false;
            return;
        }

        if (offIsTotem) {
            hadTotem = true;
            needSince = 0;
            // Safe again: optionally restore the item that was in the offhand before.
            if (restore.get() && !always.get() && !savedOffhand.isEmpty()
                    && p.getHealth() > threshold.getInt() + 4 && !(smart.get() && dangerPredicted(p))) {
                int idx = findItem(p, savedOffhand.getItem());
                if (idx >= 0 && ActionManager.claim(Channel.INVENTORY, this)) {
                    InvUtil.swapWithOffhand(idx);
                    savedOffhand = ItemStack.EMPTY;
                } else if (idx < 0) {
                    savedOffhand = ItemStack.EMPTY;
                }
            }
        } else if (!need) {
            needSince = 0;
        }
    }

    private boolean dangerPredicted(ClientPlayerEntity p) {
        float hp = CombatSafety.effectiveHealth();
        float crystal = CombatSafety.worstCrystalThreat(12.0);
        if (crystal > 0f && crystal >= hp - 1.0f) return true;
        double fall = p.fallDistance;
        if (!p.isOnGround() && fall > 3.0 && (fall - 3.0) >= hp - 1.0) return true;
        return false;
    }

    /** Totem in the hotbar or main inventory (inventory index), or -1. */
    private int findTotem(ClientPlayerEntity p) {
        return findItem(p, Items.TOTEM_OF_UNDYING);
    }

    private int findItem(ClientPlayerEntity p, Item item) {
        for (int i = 0; i < 36; i++) {
            if (InvUtil.stack(i).isOf(item)) return i;
        }
        return -1;
    }

    public int totemCount() {
        return mc.player == null ? 0 : InvUtil.countItem(Items.TOTEM_OF_UNDYING);
    }

    @Override
    public void onRender(DrawContext ctx, float tickDelta) {
        if (mc.player == null || mc.options.hudHidden) return;
        int count = totemCount();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int x = w / 2 + 100;
        int y = h - 40;
        ctx.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), x, y);
        String s = "x" + count;
        int color = count == 0 ? 0xFFFF5555 : 0xFFD8B4FE;
        ctx.drawText(mc.textRenderer, s, x + 18, y + 4, color, true);
    }

    @Override
    public String hudInfo() {
        return mc.player == null ? null : String.valueOf(totemCount());
    }

    @Override
    public void onWorldChange() {
        needSince = 0;
        savedOffhand = ItemStack.EMPTY;
        hadTotem = false;
    }
}
