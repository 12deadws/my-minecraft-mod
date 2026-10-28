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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

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
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.world == null) return;

        double range = detectRange.get();
        double reach = Targets.entityReach(6.0);
        long now = System.currentTimeMillis();
        threats = 0;

        EndCrystalEntity best = null;
        float bestDamage = -1f;
        firstSeen.keySet().removeIf(id -> mc.world.getEntityById(id) == null);

        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class,
                p.getBoundingBox().expand(range), e -> e.isAlive())) {
            Vec3d pos = new Vec3d(c.getX(), c.getY(), c.getZ());
            float self = DamageUtil.crystalDamage(p, pos);
            if (self < 1.0f) continue; // harmless to us
            threats++;
            firstSeen.putIfAbsent(c.getId(), now);

            if (!autoBreak.get()) continue;
            if (Targets.reachTo(c) > reach || !Targets.visible(c.getBoundingBox().getCenter())) continue;
            if (now - firstSeen.get(c.getId()) < (long) reaction.get()) continue;
            // Breaking detonates it in our face: only do it when the blast is within our limit and non-lethal.
            if (!CombatSafety.isSelfSafe(self, maxSelf.get(), true)) continue;
            if (self > bestDamage) {
                bestDamage = self;
                best = c;
            }
        }

        if (best != null && ActionManager.claim(Channel.ATTACK, this)) {
            mc.interactionManager.attackEntity(p, best);
            p.swingHand(Hand.MAIN_HAND);
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
