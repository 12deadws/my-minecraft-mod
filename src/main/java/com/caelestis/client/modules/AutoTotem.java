package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.CombatSafety;
import com.caelestis.client.util.InvUtil;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_332;
import net.minecraft.class_746;

/**
 * Keeps a Totem of Undying in the offhand using the normal inventory swap (the same packet as pressing F over a
 * slot). Highest-priority module: it ticks first and claims the inventory channel.
 */
public class AutoTotem extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Health Threshold", "Equip a totem when health is at or below this many HP.", 10, 1, 20, 1, " HP"));
    private final BoolSetting smart = add(new BoolSetting("Smart Mode", "Also equip when a nearby crystal or a long fall could kill you.", true));
    private final BoolSetting always = add(new BoolSetting("Always Totem", "Keep a totem in the offhand permanently.", false));
    private final NumberSetting refillDelay = add(new NumberSetting("Refill Delay", "Delay before re-equipping after a totem pops or the offhand empties.", 100, 0, 1000, 10, " ms"));
    private final BoolSetting randomDelay = add(new BoolSetting("Random Refill Delay", "Add an optional random delay when equipping or replacing a totem.", false));
    private final NumberSetting minRandomTicks = add(new NumberSetting("Minimum Random Ticks", "Shortest optional equip delay.", 1, 0, 10, 1, " t"));
    private final NumberSetting maxRandomTicks = add(new NumberSetting("Maximum Random Ticks", "Longest optional equip delay.", 3, 0, 10, 1, " t"));
    private final BoolSetting restore = add(new BoolSetting("Restore Previous Offhand Item", "Put your previous offhand item back once you are safe (not used with Always Totem).", true));

    private long needSince = 0;
    private long extraDelayMs = -1;
    private class_1799 savedOffhand = class_1799.field_8037;
    private boolean hadTotem = false;

    public AutoTotem() {
        super("Auto Totem", "Moves a Totem of Undying into your offhand when needed and refills it after a pop.", Category.PLAYER, 0);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1761 == null) return;

        class_1799 off = p.method_6079();
        boolean offIsTotem = off.method_31574(class_1802.field_8288);
        boolean need = always.get() || p.method_6032() <= threshold.getInt() || (smart.get() && dangerPredicted(p));

        if (need && !offIsTotem) {
            long now = System.currentTimeMillis();
            if (needSince == 0) {
                needSince = now;
                int lo = Math.min(minRandomTicks.getInt(), maxRandomTicks.getInt());
                int hi = Math.max(minRandomTicks.getInt(), maxRandomTicks.getInt());
                extraDelayMs = randomDelay.get() ? java.util.concurrent.ThreadLocalRandom.current().nextInt(lo, hi + 1) * 50L : 0L;
            }
            // First equip is instant; the delay only applies when refilling right after a totem was consumed.
            long wait = hadTotem ? (long) refillDelay.get() : 0L;
            if (now - needSince < wait + Math.max(0, extraDelayMs)) return;

            int src = findTotem(p);
            if (src < 0) return;
            if (!ActionManager.claim(Channel.INVENTORY, this)) return;

            if (!off.method_7960()) savedOffhand = off.method_7972();
            InvUtil.swapWithOffhand(src);
            needSince = 0;
            extraDelayMs = -1;
            hadTotem = false;
            return;
        }

        if (offIsTotem) {
            hadTotem = true;
            needSince = 0;
            extraDelayMs = -1;
            // Safe again: optionally restore the item that was in the offhand before.
            if (restore.get() && !always.get() && !savedOffhand.method_7960()
                    && p.method_6032() > threshold.getInt() + 4 && !(smart.get() && dangerPredicted(p))) {
                int idx = findItem(p, savedOffhand.method_7909());
                if (idx >= 0 && ActionManager.claim(Channel.INVENTORY, this)) {
                    InvUtil.swapWithOffhand(idx);
                    savedOffhand = class_1799.field_8037;
                } else if (idx < 0) {
                    savedOffhand = class_1799.field_8037;
                }
            }
        } else if (!need) {
            needSince = 0;
            extraDelayMs = -1;
        }
    }

    private boolean dangerPredicted(class_746 p) {
        float hp = CombatSafety.effectiveHealth();
        float crystal = CombatSafety.worstCrystalThreat(12.0);
        if (crystal > 0f && crystal >= hp - 1.0f) return true;
        double fall = p.field_6017;
        if (!p.method_24828() && fall > 3.0 && (fall - 3.0) >= hp - 1.0) return true;
        return false;
    }

    /** Totem in the hotbar or main inventory (inventory index), or -1. */
    private int findTotem(class_746 p) {
        return findItem(p, class_1802.field_8288);
    }

    private int findItem(class_746 p, class_1792 item) {
        for (int i = 0; i < 36; i++) {
            if (InvUtil.stack(i).method_31574(item)) return i;
        }
        return -1;
    }

    public int totemCount() {
        return mc.field_1724 == null ? 0 : InvUtil.countItem(class_1802.field_8288);
    }

    @Override
    public void onRender(class_332 ctx, float tickDelta) {
        if (mc.field_1724 == null || mc.field_1690.field_1842) return;
        int count = totemCount();
        int w = ctx.method_51421();
        int h = ctx.method_51443();
        int x = w / 2 + 100;
        int y = h - 40;
        ctx.method_51427(new class_1799(class_1802.field_8288), x, y);
        String s = "x" + count;
        int color = count == 0 ? 0xFFFF5555 : 0xFFD8B4FE;
        ctx.method_51433(mc.field_1772, s, x + 18, y + 4, color, true);
    }

    @Override
    public String hudInfo() {
        return mc.field_1724 == null ? null : String.valueOf(totemCount());
    }

    @Override
    public void onWorldChange() {
        needSince = 0;
        extraDelayMs = -1;
        savedOffhand = class_1799.field_8037;
        hadTotem = false;
    }
}
