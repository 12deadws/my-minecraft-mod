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
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1893;
import net.minecraft.class_239;
import net.minecraft.class_3966;
import net.minecraft.class_746;

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
    private final BoolSetting randomSwitchDelay = add(new BoolSetting("Random Switch Delay", "Randomize the axe-to-mace delay between 1 and 3 ticks.", false));
    private final NumberSetting maceHoldTicks = add(new NumberSetting("Mace Hold Ticks", "Show the mace in hand for this many ticks before the slam.", 2, 1, 5, 1, " t"));
    private final NumberSetting cooldown = add(new NumberSetting("Sequence Cooldown", "Minimum time between two slam sequences.", 600, 100, 2000, 50, " ms"));

    private enum State {IDLE, WAIT, MACE_READY}

    private State state = State.IDLE;
    private int ticksLeft;
    private int prevSlot = -1;
    private int maceSlot = -1;
    private class_1657 target;
    private long lastSequence = 0;

    public AutoStunSlam() {
        super("Auto Stun Slam", "Axe breaks the shield, then the mace follows up. Restores your previous hotbar slot.", Category.COMBAT, 40);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1761 == null) return;

        if (state == State.WAIT) {
            ActionManager.hold(Channel.ATTACK, this, 1);
            ActionManager.hold(Channel.HOTBAR, this, 1);
            if (target == null || !Targets.isValidEnemy(target) || Targets.reachTo(target) > p.method_55755()) {
                abort();
                return;
            }
            if (--ticksLeft <= 0) readyMace(p);
            return;
        }
        if (state == State.MACE_READY) {
            ActionManager.hold(Channel.ATTACK, this, 1);
            ActionManager.hold(Channel.HOTBAR, this, 1);
            if (target == null || !Targets.isValidEnemy(target) || Targets.reachTo(target) > p.method_55755()) {
                abort();
                return;
            }
            if (--ticksLeft <= 0) finish(p);
            return;
        }

        if (System.currentTimeMillis() - lastSequence < (long) cooldown.get()) return;
        if (!ActionManager.isFree(Channel.ATTACK, this) || !ActionManager.isFree(Channel.HOTBAR, this)) return;
        if (p.method_6115()) return;

        if (!(mc.field_1765 instanceof class_3966 ehr) || ehr.method_17783() != class_239.class_240.field_1331) return;
        if (!(ehr.method_17782() instanceof class_1657 t) || !Targets.isValidEnemy(t)) return;
        if (!t.method_6039()) return;
        if (Targets.reachTo(t) > p.method_55755() || !Targets.canSee(t)) return;

        if (!autoActivate.get() && !mc.field_1690.field_1886.method_1434()) return;
        if (requireFall.get() && p.field_6017 < minFall.get()) return;

        int axe = InvUtil.findHotbar(ItemUtil::isAxe);
        int mace = bestMace(t, p);
        if (axe < 0 || mace < 0) return;

        // The axe only disables the shield on a (near) full-strength swing.
        if (InvUtil.selected() == axe && p.method_7261(0.0f) < 0.9f) return;

        if (!ActionManager.hold(Channel.ATTACK, this, 0) || !ActionManager.hold(Channel.HOTBAR, this, 0)) return;
        ActionManager.claim(Channel.INVENTORY, this);

        target = t;
        maceSlot = mace;
        prevSlot = InvUtil.selected();

        InvUtil.select(axe);
        mc.field_1761.method_2918(p, t);
        p.method_6104(class_1268.field_5808);

        int delay = randomSwitchDelay.get() ? java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 4) : switchDelay.getInt();
        if (delay <= 0) {
            readyMace(p);
        } else {
            state = State.WAIT;
            ticksLeft = delay;
            ActionManager.hold(Channel.ATTACK, this, delay);
            ActionManager.hold(Channel.HOTBAR, this, delay);
        }
    }

    private void readyMace(class_746 p) {
        if (maceSlot < 0 || !ItemUtil.isMace(InvUtil.stack(maceSlot))) { abort(); return; }
        InvUtil.select(maceSlot);
        state = State.MACE_READY;
        ticksLeft = maceHoldTicks.getInt();
        ActionManager.hold(Channel.ATTACK, this, ticksLeft + 1);
        ActionManager.hold(Channel.HOTBAR, this, ticksLeft + 1);
    }

    private void finish(class_746 p) {
        try {
            if (target != null && target.method_5805() && maceSlot >= 0 && ItemUtil.isMace(InvUtil.stack(maceSlot))) {
                mc.field_1761.method_2918(p, target);
                p.method_6104(class_1268.field_5808);
            }
        } finally {
            abort();
            lastSequence = System.currentTimeMillis();
        }
    }

    private void abort() {
        if (prevSlot >= 0 && mc.field_1724 != null) InvUtil.select(prevSlot);
        prevSlot = -1;
        maceSlot = -1;
        target = null;
        state = State.IDLE;
        ActionManager.release(Channel.ATTACK, this);
        ActionManager.release(Channel.HOTBAR, this);
    }

    /** Picks the mace with the better enchantment for this situation (Breach vs armour, Density vs fall height). */
    private int bestMace(class_1657 t, class_746 p) {
        int best = -1;
        double bestScore = -1;
        for (int i = 0; i < 9; i++) {
            class_1799 s = InvUtil.stack(i);
            if (!ItemUtil.isMace(s)) continue;
            int density = ItemUtil.enchantLevel(s, class_1893.field_50157);
            int breach = ItemUtil.enchantLevel(s, class_1893.field_50158);
            double fall = Math.max(0.0, p.field_6017);
            double densityBonus = density * 0.5 * fall;               // +0.5 damage per block per level
            double breachBonus = breach * 0.15 * t.method_6096() * 0.5;   // 15% armour reduction per level (rough)
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
        if (state != State.IDLE) abort();
        state = State.IDLE;
    }

    @Override
    public void onWorldChange() {
        if (state != State.IDLE) abort();
        state = State.IDLE;
        prevSlot = -1;
        maceSlot = -1;
        target = null;
    }
}
