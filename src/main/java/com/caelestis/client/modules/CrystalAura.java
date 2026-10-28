package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.EnumSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.BlockUtil;
import com.caelestis.client.util.CombatSafety;
import com.caelestis.client.util.DamageUtil;
import com.caelestis.client.util.InvUtil;
import com.caelestis.client.util.Targets;
import java.util.Comparator;
import java.util.List;
import net.minecraft.class_1268;
import net.minecraft.class_1511;
import net.minecraft.class_1657;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3965;
import net.minecraft.class_746;

/**
 * Places and breaks End Crystals against nearby enemies. Every placement and every break is gated by the shared
 * damage model (DamageUtil / CombatSafety), vanilla reach and line of sight.
 */
public class CrystalAura extends Module {
    private final NumberSetting placeSpeed = add(new NumberSetting("Place Speed", "Crystal placements per second.", 8, 1, 20, 1, "/s"));
    private final NumberSetting breakSpeed = add(new NumberSetting("Break Speed", "Crystal hits per second.", 10, 1, 20, 1, "/s"));
    private final NumberSetting targetRange = add(new NumberSetting("Target Range", "Enemies further than this are ignored.", 10, 3, 16, 1, " m"));
    private final NumberSetting placeRange = add(new NumberSetting("Place Range", "Max placement reach (capped at vanilla block reach).", 4.5, 1.0, 6.0, 0.5, " m"));
    private final NumberSetting breakRange = add(new NumberSetting("Break Range", "Max break reach (capped at vanilla entity reach).", 3.0, 1.0, 6.0, 0.5, " m"));
    private final NumberSetting minDamage = add(new NumberSetting("Min Target Damage", "Only act if the target would take at least this much.", 5.0, 0.5, 36.0, 0.5, " HP"));
    private final NumberSetting maxSelf = add(new NumberSetting("Max Self Damage", "Never act if you would take more than this.", 6.0, 0.0, 36.0, 0.5, " HP"));
    private final BoolSetting antiSuicide = add(new BoolSetting("Anti-Suicide", "Never act if the explosion could kill you.", true));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch to Crystals", "Switch to a hotbar crystal stack if you are not holding one.", true));
    private final BoolSetting restoreSlot = add(new BoolSetting("Restore Previous Slot", "Switch back to your previous slot after placing.", true));
    private final BoolSetting facePlace = add(new BoolSetting("Face Placement", "Also consider crystals placed at head height next to the target.", false));
    private final BoolSetting pauseEating = add(new BoolSetting("Pause While Eating", "Do nothing while using an item (eating, drinking, blocking).", true));
    private final EnumSetting priority = add(new EnumSetting("Target Priority", "Which enemy to focus.", "Closest", "Closest", "Lowest Health", "Best Damage"));

    private long lastPlace = 0, lastBreak = 0;

    public CrystalAura() {
        super("Crystal Aura", "Places and breaks End Crystals near enemies with damage and self-damage checks.", Category.CRYSTAL, 50);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1761 == null || mc.field_1687 == null) return;
        if (pauseEating.get() && p.method_6115()) return;

        List<class_1657> targets = Targets.enemies(targetRange.get());
        if (targets.isEmpty()) return;
        sortTargets(targets, p);
        if (targets.size() > 3) targets = targets.subList(0, 3);

        long now = System.currentTimeMillis();

        if (now - lastBreak >= 1000L / breakSpeed.getInt() && tryBreak(p, targets)) {
            lastBreak = now;
            return; // one action per tick keeps the sequence predictable
        }
        if (now - lastPlace >= 1000L / placeSpeed.getInt() && tryPlace(p, targets)) {
            lastPlace = now;
        }
    }

    private void sortTargets(List<class_1657> list, class_746 p) {
        // "Best Damage" evaluates the 3 nearest enemies and the scoring below picks the highest-damage option;
        // "Closest" and "Lowest Health" decide which enemies make the shortlist first.
        if (priority.is("Lowest Health")) {
            list.sort(Comparator.comparingDouble(e -> e.method_6032() + e.method_6067()));
        } else {
            list.sort(Comparator.comparingDouble(p::method_5739));
        }
    }

    // ------------------------------------------------------------------ break

    private boolean tryBreak(class_746 p, List<class_1657> targets) {
        double reach = Targets.entityReach(breakRange.get());
        class_238 area = p.method_5829().method_1014(reach + 1.0);
        class_1511 best = null;
        double bestScore = -Double.MAX_VALUE;

        for (class_1511 c : mc.field_1687.method_8390(class_1511.class, area, e -> e.method_5805())) {
            if (Targets.reachTo(c) > reach) continue;
            if (!Targets.visible(c.method_5829().method_1005())) continue;

            class_243 pos = new class_243(c.method_23317(), c.method_23318(), c.method_23321());
            float self = DamageUtil.crystalDamage(p, pos);
            if (!CombatSafety.isSelfSafe(self, maxSelf.get(), antiSuicide.get())) continue;

            float dealt = 0f;
            for (class_1657 t : targets) dealt = Math.max(dealt, DamageUtil.crystalDamage(t, pos));
            if (dealt < minDamage.get()) continue;

            double score = dealt - self;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        if (best == null) return false;
        if (!ActionManager.claim(Channel.ATTACK, this)) return false;

        mc.field_1761.method_2918(p, best);
        p.method_6104(class_1268.field_5808);
        return true;
    }

    // ------------------------------------------------------------------ place

    private boolean tryPlace(class_746 p, List<class_1657> targets) {
        boolean offhand = p.method_6079().method_31574(class_1802.field_8301);
        boolean mainHand = p.method_6047().method_31574(class_1802.field_8301);
        int hotbar = InvUtil.findHotbarItem(class_1802.field_8301);
        if (!offhand && !mainHand && !(autoSwitch.get() && hotbar >= 0)) return false;

        double reach = Targets.blockReach(placeRange.get());
        class_243 eye = p.method_33571();

        class_2338 bestBase = null;
        double bestScore = -Double.MAX_VALUE;

        for (class_1657 t : targets) {
            class_2338 feet = t.method_24515();
            int yMin = -1;
            int yMax = facePlace.get() ? 1 : 0;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int dy = yMin; dy <= yMax; dy++) {
                        class_2338 base = feet.method_10069(dx, dy, dz);
                        if (eye.method_1022(class_243.method_24954(base).method_1031(0.5, 1.0, 0.5)) > reach) continue;
                        if (!BlockUtil.isObsidianOrBedrock(mc.field_1687.method_8320(base))) continue;
                        if (!mc.field_1687.method_8320(base.method_10084()).method_26215() || !mc.field_1687.method_8320(base.method_10086(2)).method_26215()) continue;
                        if (!crystalFits(base)) continue;

                        class_243 crystalPos = new class_243(base.method_10263() + 0.5, base.method_10264() + 1.0, base.method_10260() + 0.5);
                        float dealt = DamageUtil.crystalDamage(t, crystalPos);
                        if (dealt < minDamage.get()) continue;
                        float self = DamageUtil.crystalDamage(p, crystalPos);
                        if (!CombatSafety.isSelfSafe(self, maxSelf.get(), antiSuicide.get())) continue;

                        double score = dealt - self * 1.5;
                        if (score > bestScore) {
                            bestScore = score;
                            bestBase = base;
                        }
                    }
                }
            }
        }
        if (bestBase == null) return false;
        if (!ActionManager.isFree(Channel.HOTBAR, this)) return false;

        class_3965 hit = new class_3965(class_243.method_24954(bestBase).method_1031(0.5, 1.0, 0.5), class_2350.field_11036, bestBase, false);

        if (offhand) {
            mc.field_1761.method_2896(p, class_1268.field_5810, hit);
            p.method_6104(class_1268.field_5810);
            return true;
        }
        int prev = InvUtil.selected();
        int slot = mainHand ? prev : hotbar;
        if (!ActionManager.claim(Channel.HOTBAR, this)) return false;
        if (slot != prev) InvUtil.select(slot);
        mc.field_1761.method_2896(p, class_1268.field_5808, hit);
        p.method_6104(class_1268.field_5808);
        if (slot != prev && restoreSlot.get()) InvUtil.select(prev);
        return true;
    }

    /** Vanilla placement rule: the 1x2x1 space above the base must contain no entities. */
    private boolean crystalFits(class_2338 base) {
        class_2338 above = base.method_10084();
        class_238 box = new class_238(above.method_10263(), above.method_10264(), above.method_10260(), above.method_10263() + 1, above.method_10264() + 2, above.method_10260() + 1);
        return mc.field_1687.method_8333(null, box, e -> e.method_5805() && !e.method_7325()).isEmpty();
    }
}
