package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.CombatSafety;
import com.caelestis.client.util.DamageUtil;
import com.caelestis.client.util.Targets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1268;
import net.minecraft.class_1511;
import net.minecraft.class_243;
import net.minecraft.class_746;

/**
 * Defensive crystal handling. Finds crystals that would hurt you, and (optionally) breaks them when that is within
 * vanilla reach and the blast you would take is below your configured limit. Uses the same DamageUtil /
 * CombatSafety as Crystal Aura, and ticks before it so its decision wins the attack channel.
 */
public class AntiCrystal extends Module {
    private final NumberSetting detectRange = add(new NumberSetting("Detection Range", "Crystals within this range are evaluated.", 8, 2, 16, 1, " m"));
    private final BoolSetting autoBreak = add(new BoolSetting("Auto Break", "Break dangerous crystals that are within vanilla reach.", true));
    private final NumberSetting maxSelf = add(new NumberSetting("Max Self Damage", "Do not break a crystal if the blast would hurt you more than this.", 6.0, 0.0, 36.0, 0.5, " HP"));
    private final NumberSetting reaction = add(new NumberSetting("Reaction Delay", "Delay between spotting a dangerous crystal and attacking it.", 50, 0, 500, 10, " ms"));

    private final Map<Integer, Long> firstSeen = new HashMap<>();
    private int threats = 0;

    public AntiCrystal() {
        super("Anti Crystal", "Detects crystals that threaten you and breaks them when it is safe and in reach.", Category.CRYSTAL, 10);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1761 == null || mc.field_1687 == null) return;

        double range = detectRange.get();
        double reach = Targets.entityReach(6.0);
        long now = System.currentTimeMillis();
        threats = 0;

        class_1511 best = null;
        float bestDamage = -1f;
        firstSeen.keySet().removeIf(id -> mc.field_1687.method_8469(id) == null);

        for (class_1511 c : mc.field_1687.method_8390(class_1511.class,
                p.method_5829().method_1014(range), e -> e.method_5805())) {
            class_243 pos = new class_243(c.method_23317(), c.method_23318(), c.method_23321());
            float self = DamageUtil.crystalDamage(p, pos);
            if (self < 1.0f) continue; // harmless to us
            threats++;
            firstSeen.putIfAbsent(c.method_5628(), now);

            if (!autoBreak.get()) continue;
            if (Targets.reachTo(c) > reach || !Targets.visible(c.method_5829().method_1005())) continue;
            if (now - firstSeen.get(c.method_5628()) < (long) reaction.get()) continue;
            // Breaking detonates it in our face: only do it when the blast is within our limit and non-lethal.
            if (!CombatSafety.isSelfSafe(self, maxSelf.get(), true)) continue;
            if (self > bestDamage) {
                bestDamage = self;
                best = c;
            }
        }

        if (best != null && ActionManager.claim(Channel.ATTACK, this)) {
            mc.field_1761.method_2918(p, best);
            p.method_6104(class_1268.field_5808);
        }
    }

    @Override
    public String hudInfo() {
        return threats > 0 ? threats + " threat" + (threats == 1 ? "" : "s") : null;
    }

    @Override
    public void onWorldChange() {
        firstSeen.clear();
        threats = 0;
    }
}
