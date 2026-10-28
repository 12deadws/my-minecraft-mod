package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.BlockUtil;
import com.caelestis.client.util.CombatSafety;
import com.caelestis.client.util.DamageUtil;
import com.caelestis.client.util.InvUtil;
import com.caelestis.client.util.Targets;
import java.util.List;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_4969;
import net.minecraft.class_746;

/**
 * Place -> charge with glowstone -> detonate a Respawn Anchor next to an enemy, with damage / self-damage checks.
 * Hard guard: it never runs in the Nether, where a charged anchor is just a respawn point and does not explode.
 * It only works in the Overworld and the End (and other non-Nether dimensions).
 */
public class AnchorAura extends Module {
    private final NumberSetting anchorRange = add(new NumberSetting("Anchor Range", "Max distance to the anchor (capped at vanilla block reach).", 4.5, 1.0, 6.0, 0.5, " m"));
    private final NumberSetting placeDelay = add(new NumberSetting("Placement Delay", "Time before placing an anchor / charging it.", 100, 0, 1000, 10, " ms"));
    private final NumberSetting activateDelay = add(new NumberSetting("Activation Delay", "Time between charging and detonating.", 100, 0, 1000, 10, " ms"));
    private final NumberSetting minDamage = add(new NumberSetting("Min Target Damage", "Only detonate if the target would take at least this much.", 6.0, 0.5, 36.0, 0.5, " HP"));
    private final NumberSetting maxSelf = add(new NumberSetting("Max Self Damage", "Never detonate if you would take more than this.", 4.0, 0.0, 36.0, 0.5, " HP"));
    private final BoolSetting antiSuicide = add(new BoolSetting("Anti-Suicide", "Never detonate if the blast could kill you.", true));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch", "Switch to anchors / glowstone in the hotbar as needed.", true));
    private final BoolSetting restoreSlot = add(new BoolSetting("Restore Slot", "Switch back to your previous slot afterwards.", true));

    private class_2338 anchorPos;
    private long stepAt = 0;
    private long startedAt = 0;
    private boolean inNether = false;

    public AnchorAura() {
        super("Anchor Aura", "Explodes Respawn Anchors on enemies (Overworld / End only, never in the Nether).", Category.CRYSTAL, 55);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1687 == null || mc.field_1761 == null) return;

        // Hard dimension guard: anchors are respawn points in the Nether and do not explode there.
        inNether = mc.field_1687.method_27983() == class_1937.field_25180;
        if (inNether) {
            anchorPos = null;
            return;
        }

        List<class_1657> targets = Targets.enemies(10.0);
        if (targets.isEmpty()) {
            anchorPos = null; // stop when there are no valid targets
            return;
        }
        if (anchorPos != null && System.currentTimeMillis() - startedAt > 4000) anchorPos = null; // stale sequence
        if (System.currentTimeMillis() < stepAt) return;
        if (!ActionManager.isFree(Channel.HOTBAR, this)) return;

        double reach = Targets.blockReach(anchorRange.get());

        if (anchorPos == null) {
            anchorPos = chooseSpot(p, targets, reach);
            if (anchorPos == null) return;
            startedAt = System.currentTimeMillis();
        }

        class_2680 state = mc.field_1687.method_8320(anchorPos);
        if (!BlockUtil.isInRange(anchorPos, reach + 0.5)) {
            anchorPos = null;
            return;
        }

        if (state.method_27852(class_2246.field_23152)) {
            int charges = state.method_11654(class_4969.field_23153);
            if (charges == 0) {
                if (charge(p)) stepAt = System.currentTimeMillis() + (long) activateDelay.get();
                return;
            }
            detonate(p, targets);
            return;
        }

        if (state.method_45474()) {
            if (place(p)) stepAt = System.currentTimeMillis() + (long) placeDelay.get();
            else anchorPos = null;
        } else {
            anchorPos = null; // something else is there now
        }
    }

    private class_2338 chooseSpot(class_746 p, List<class_1657> targets, double reach) {
        if (InvUtil.findHotbarItem(class_1802.field_23141) < 0 && !p.method_6047().method_31574(class_1802.field_23141)) return null;
        if (InvUtil.findHotbarItem(class_1802.field_8801) < 0) return null;
        // Offhand glowstone would be consumed instead of detonating, so refuse in that case.
        if (p.method_6079().method_31574(class_1802.field_8801)) return null;

        class_2338 best = null;
        double bestScore = -Double.MAX_VALUE;
        for (class_1657 t : targets) {
            class_2338 feet = t.method_24515();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        class_2338 pos = feet.method_10069(dx, dy, dz);
                        if (!BlockUtil.isInRange(pos, reach)) continue;
                        if (!mc.field_1687.method_8320(pos).method_45474() || !BlockUtil.canPlaceBlockAt(pos)) continue;
                        if (BlockUtil.findPlacement(pos, reach) == null) continue;

                        class_243 center = class_243.method_24953(pos);
                        float dealt = DamageUtil.explosionDamage(t, center, DamageUtil.ANCHOR_POWER, pos);
                        if (dealt < minDamage.get()) continue;
                        float self = DamageUtil.explosionDamage(p, center, DamageUtil.ANCHOR_POWER, pos);
                        if (!CombatSafety.isSelfSafe(self, maxSelf.get(), antiSuicide.get())) continue;

                        double score = dealt - self * 1.5;
                        if (score > bestScore) {
                            bestScore = score;
                            best = pos;
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean place(class_746 p) {
        if (!ActionManager.claim(Channel.HOTBAR, this)) return false;
        return BlockUtil.placeBlock(anchorPos, s -> s.method_31574(class_1802.field_23141), autoSwitch.get(), restoreSlot.get());
    }

    private boolean charge(class_746 p) {
        int slot = p.method_6047().method_31574(class_1802.field_8801) ? InvUtil.selected() : InvUtil.findHotbarItem(class_1802.field_8801);
        if (slot < 0) return false;
        if (slot != InvUtil.selected() && !autoSwitch.get()) return false;
        if (!ActionManager.claim(Channel.HOTBAR, this)) return false;

        int prev = InvUtil.selected();
        InvUtil.select(slot);
        mc.field_1761.method_2896(p, class_1268.field_5808, clickOn(anchorPos));
        p.method_6104(class_1268.field_5808);
        if (restoreSlot.get()) InvUtil.select(prev);
        return true;
    }

    private void detonate(class_746 p, List<class_1657> targets) {
        // Re-check the damage model right before detonating (things move).
        class_243 center = class_243.method_24953(anchorPos);
        float dealt = 0f;
        for (class_1657 t : targets) dealt = Math.max(dealt, DamageUtil.explosionDamage(t, center, DamageUtil.ANCHOR_POWER, anchorPos));
        float self = DamageUtil.explosionDamage(p, center, DamageUtil.ANCHOR_POWER, anchorPos);
        if (dealt < minDamage.get() || !CombatSafety.isSelfSafe(self, maxSelf.get(), antiSuicide.get())) {
            anchorPos = null; // unfavourable: walk away from this anchor
            return;
        }
        if (p.method_6079().method_31574(class_1802.field_8801)) return;

        // Detonation needs a non-glowstone item in the main hand.
        int prev = InvUtil.selected();
        int slot = p.method_6047().method_31574(class_1802.field_8801) ? nonGlowstoneSlot() : prev;
        if (slot < 0) return;
        if (slot != prev && !autoSwitch.get()) return;
        if (!ActionManager.claim(Channel.HOTBAR, this)) return;

        if (slot != prev) InvUtil.select(slot);
        mc.field_1761.method_2896(p, class_1268.field_5808, clickOn(anchorPos));
        p.method_6104(class_1268.field_5808);
        if (slot != prev && restoreSlot.get()) InvUtil.select(prev);
        anchorPos = null;
        stepAt = System.currentTimeMillis() + (long) placeDelay.get();
    }

    private int nonGlowstoneSlot() {
        for (int i = 0; i < 9; i++) {
            if (!InvUtil.stack(i).method_31574(class_1802.field_8801)) return i;
        }
        return -1;
    }

    private class_3965 clickOn(class_2338 pos) {
        return new class_3965(class_243.method_24953(pos).method_1031(0, 0.5, 0), class_2350.field_11036, pos, false);
    }

    @Override
    public String hudInfo() {
        return inNether ? "inactive: Nether" : null;
    }

    @Override
    public void onDisable() {
        anchorPos = null;
    }

    @Override
    public void onWorldChange() {
        anchorPos = null;
        inNether = false;
    }
}
