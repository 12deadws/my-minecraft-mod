package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.ItemUtil;
import com.caelestis.client.util.Targets;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Reaction delay after a target is under the crosshair and your cooldown is ready.", 100, 0, 500, 10, " ms"));
    private final BoolSetting critOnly = add(new BoolSetting("Critical Hits Only", "Only attack while a vanilla critical hit is possible (falling, not sprinting).", false));
    private final BoolSetting allowFist = add(new BoolSetting("Allow Fist Attacks", "Also attack with an empty hand. Off by default.", false));

    private long aimedSince = 0;
    private PlayerEntity lastTarget;

    public TriggerBot() {
        super("TriggerBot", "Attacks the player under your crosshair with real attacks, respecting cooldown, reach and line of sight.", Category.COMBAT, 60);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        HitResult hr = mc.crosshairTarget;
        if (!(hr instanceof EntityHitResult ehr)) {
            aimedSince = 0;
            return;
        }
        Entity e = ehr.getEntity();
        if (!(e instanceof PlayerEntity target) || !Targets.isValidEnemy(target)) {
            aimedSince = 0;
            return;
        }
        if (target != lastTarget) {
            aimedSince = 0;
            lastTarget = target;
        }
        if (!weaponAllowed(p.getMainHandStack())) {
            aimedSince = 0;
            return;
        }
        if (p.isUsingItem()) return;

        // Real reach + line of sight (crosshair target is already vanilla-validated; double check).
        if (Targets.reachTo(target) > p.getEntityInteractionRange()) return;
        if (!Targets.canSee(target)) return;

        long now = System.currentTimeMillis();
        if (aimedSince == 0) aimedSince = now;

        if (p.getAttackCooldownProgress(0.0f) < 0.95f) return;
        if (now - aimedSince < (long) delay.get()) return;
        if (critOnly.get() && !canCrit(p)) return;

        // Do not fight over the attack action with a higher-priority module this tick.
        if (!ActionManager.claim(ActionManager.Channel.ATTACK, this)) return;

        Targets.prefer(target);
        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        aimedSince = now; // next swing waits for cooldown + delay again
    }

    private boolean weaponAllowed(ItemStack s) {
        if (s.isEmpty()) return allowFist.get();
        return ItemUtil.isSword(s) || ItemUtil.isAxe(s);
    }

    /** Vanilla critical-hit conditions. */
    private boolean canCrit(ClientPlayerEntity p) {
        return !p.isOnGround() && p.fallDistance > 0.0 && !p.isClimbing() && !p.isTouchingWater()
                && !p.hasStatusEffect(StatusEffects.BLINDNESS) && !p.hasVehicle() && !p.isSprinting();
    }

    @Override
    public void onDisable() {
        aimedSince = 0;
        lastTarget = null;
    }

    @Override
    public void onWorldChange() {
        onDisable();
    }
}
