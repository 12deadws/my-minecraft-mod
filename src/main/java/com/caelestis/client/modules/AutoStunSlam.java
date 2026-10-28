package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.InvUtil;
import com.caelestis.client.util.ItemUtil;
import com.caelestis.client.util.Targets;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Axe -> mace stun slam. Against a shield-blocking player: select the axe, attack (disables the shield), then
 * select the mace and attack again, then restore the original hotbar slot. With a switch delay of 0 the whole
 * sequence is sent within a single client tick, which vanilla servers process in order.
 *
 * Server-side limits (see README): the axe only disables a shield with a (near) full-strength swing, and
 * switching items resets the attack cooldown, so the mace follow-up hit is cooldown-scaled by the server.
 * The mace's fall-based smash bonus still requires > 1.5 blocks of fall distance.
 */
public class AutoStunSlam extends Module {
    private final BoolSetting autoActivate = add(new BoolSetting("Auto Activate", "Start automatically against a shield user under your crosshair. Off = only while you hold attack.", false));
    private final BoolSetting requireFall = add(new BoolSetting("Require Fall", "Only slam while falling far enough for a mace smash.", true));
    private final NumberSetting minFall = add(new NumberSetting("Min Fall Distance", "Fall distance (blocks) before a smash counts. Vanilla needs more than 1.5.", 1.5, 0.0, 10.0, 0.1));
    private final NumberSetting switchDelay = add(new NumberSetting("Switch Delay", "Ticks between the axe hit and switching to the mace. 0 = same tick.", 0, 0, 5, 1, " t"));
    private final NumberSetting cooldown = add(new NumberSetting("Sequence Cooldown", "Minimum time between two slam sequences.", 600, 100, 2000, 50, " ms"));

    private enum State {IDLE, WAIT}

    private State state = State.IDLE;
    private int ticksLeft;
    private int prevSlot = -1;
    private int maceSlot = -1;
    private PlayerEntity target;
    private long lastSequence = 0;

    public AutoStunSlam() {
        super("Auto Stun Slam", "Axe breaks the shield, then the mace follows up. Restores your previous hotbar slot.", Category.COMBAT, 40);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        if (state == State.WAIT) {
            ActionManager.hold(Channel.ATTACK, this, 1);
            ActionManager.hold(Channel.HOTBAR, this, 1);
            if (target == null || !Targets.isValidEnemy(target) || Targets.reachTo(target) > p.getEntityInteractionRange()) {
                abort();
                return;
            }
            if (--ticksLeft <= 0) finish(p);
            return;
        }

        if (System.currentTimeMillis() - lastSequence < (long) cooldown.get()) return;
        if (!ActionManager.isFree(Channel.ATTACK, this) || !ActionManager.isFree(Channel.HOTBAR, this)) return;
        if (p.isUsingItem()) return;

        if (!(mc.crosshairTarget instanceof EntityHitResult ehr) || ehr.getType() != HitResult.Type.ENTITY) return;
        if (!(ehr.getEntity() instanceof PlayerEntity t) || !Targets.isValidEnemy(t)) return;
        if (!t.isBlocking()) return;
        if (Targets.reachTo(t) > p.getEntityInteractionRange() || !Targets.canSee(t)) return;

        if (!autoActivate.get() && !mc.options.attackKey.isPressed()) return;
        if (requireFall.get() && p.fallDistance < minFall.get()) return;

        int axe = InvUtil.findHotbar(ItemUtil::isAxe);
        int mace = bestMace(t, p);
        if (axe < 0 || mace < 0) return;

        // The axe only disables the shield on a (near) full-strength swing.
        if (InvUtil.selected() == axe && p.getAttackCooldownProgress(0.0f) < 0.9f) return;

        if (!ActionManager.hold(Channel.ATTACK, this, 0) || !ActionManager.hold(Channel.HOTBAR, this, 0)) return;
        ActionManager.claim(Channel.INVENTORY, this);

        target = t;
        maceSlot = mace;
        prevSlot = InvUtil.selected();

        InvUtil.select(axe);
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);

        int delay = switchDelay.getInt();
        if (delay <= 0) {
            finish(p);
        } else {
            state = State.WAIT;
            ticksLeft = delay;
            ActionManager.hold(Channel.ATTACK, this, delay);
            ActionManager.hold(Channel.HOTBAR, this, delay);
        }
    }

    private void finish(ClientPlayerEntity p) {
        try {
            if (target != null && target.isAlive() && maceSlot >= 0 && ItemUtil.isMace(InvUtil.stack(maceSlot))) {
                InvUtil.select(maceSlot);
                mc.interactionManager.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
            }
        } finally {
            abort();
            lastSequence = System.currentTimeMillis();
        }
    }

    private void abort() {
        if (prevSlot >= 0 && mc.player != null) InvUtil.select(prevSlot);
        prevSlot = -1;
        maceSlot = -1;
        target = null;
        state = State.IDLE;
        ActionManager.release(Channel.ATTACK, this);
        ActionManager.release(Channel.HOTBAR, this);
    }

    /** Picks the mace with the better enchantment for this situation (Breach vs armour, Density vs fall height). */
    private int bestMace(PlayerEntity t, ClientPlayerEntity p) {
        int best = -1;
        double bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = InvUtil.stack(i);
            if (!ItemUtil.isMace(s)) continue;
            int density = ItemUtil.enchantLevel(s, Enchantments.DENSITY);
            int breach = ItemUtil.enchantLevel(s, Enchantments.BREACH);
            double fall = Math.max(0.0, p.fallDistance);
            double densityBonus = density * 0.5 * fall;               // +0.5 damage per block per level
            double breachBonus = breach * 0.15 * t.getArmor() * 0.5;   // 15% armour reduction per level (rough)
            double score = densityBonus + breachBonus;
            if (best < 0 || score > bestScore) {
                best = i;
                bestScore = score;
            }
        }
        return best;
    }

    @Override
    public void onDisable() {
        if (state == State.WAIT) abort();
        state = State.IDLE;
    }

    @Override
    public void onWorldChange() {
        state = State.IDLE;
        prevSlot = -1;
        maceSlot = -1;
        target = null;
    }
}
