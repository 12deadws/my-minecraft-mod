package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.InvUtil;
import com.caelestis.client.util.ItemUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.List;

/** Splash Instant Health only; gradual, slightly imperfect pitch motion with a safe slot restore. */
public class AutoHealthPot extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Health Threshold", "Throw when health is at or below this many HP.", 8, 1, 20, 1, " HP"));
    private final NumberSetting cooldown = add(new NumberSetting("Cooldown", "Minimum time between throws.", 600, 100, 3000, 50, " ms"));
    private final NumberSetting potionSlot = add(new NumberSetting("Potion Hotbar Slot", "Dedicated slot (1-9) to use and refill with healing splash potions.", 7, 1, 9, 1));
    private final NumberSetting lookDownTicks = add(new NumberSetting("Look Down Ticks", "Ticks spent aiming at the ground.", 6, 3, 12, 1));
    private final NumberSetting lookBackTicks = add(new NumberSetting("Look Back Ticks", "Ticks spent returning to the original opponent-facing angle.", 6, 3, 12, 1));
    private final NumberSetting restockDelay = add(new NumberSetting("Restock Delay", "Minimum time between inventory swaps.", 350, 100, 1500, 50, " ms"));

    private enum Phase { IDLE, LOOK_DOWN, LOOK_BACK }
    private Phase phase = Phase.IDLE;
    private long lastThrow, lastRestock;
    private int thrown, phaseTick, originalSlot = -1, activeSlot = -1;
    private float originalPitch;

    public AutoHealthPot() {
        super("Auto Health Pot", "Looks down, throws healing splash potions, looks back, and refills a configurable hotbar slot.", Category.COMBAT, 20);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || !p.isAlive()) {
            cancelSequence();
            return;
        }
        if (phase != Phase.IDLE) {
            advanceSequence(p);
            return;
        }

        // Restocking is independent of health and does one vanilla inventory swap at a time.
        refillDedicatedSlot();
        if (p.getHealth() > threshold.getInt()) return;
        if (System.currentTimeMillis() - lastThrow < (long) cooldown.get()) return;
        if (potionAlreadyAtFeet(p)) return;
        int slot = potionSlot.getInt() - 1;
        if (!ItemUtil.isHealingSplash(InvUtil.stack(slot))) return;
        if (!ActionManager.isFree(Channel.HOTBAR, this) || !ActionManager.isFree(Channel.ROTATION, this)) return;
        if (!ActionManager.hold(Channel.HOTBAR, this, 30)) return;
        if (!ActionManager.hold(Channel.ROTATION, this, 30)) {
            ActionManager.release(Channel.HOTBAR, this);
            return;
        }

        originalSlot = InvUtil.selected();
        originalPitch = p.getPitch();
        activeSlot = slot;
        phaseTick = 0;
        phase = Phase.LOOK_DOWN;
        // Select only after successfully obtaining ownership of the hotbar/rotation channels.
        InvUtil.select(activeSlot);
    }

    private void advanceSequence(ClientPlayerEntity p) {
        // Other modules may be scheduled ahead of us; never override their actions if a channel was lost.
        if (!ActionManager.isFree(Channel.HOTBAR, this) || !ActionManager.isFree(Channel.ROTATION, this)) {
            cancelSequence();
            return;
        }
        ActionManager.claim(Channel.HOTBAR, this);
        ActionManager.claim(Channel.ROTATION, this);

        // A changed slot means the player intervened; don't throw an unexpected item.
        if (phase == Phase.LOOK_DOWN && InvUtil.selected() != activeSlot) {
            cancelSequence();
            return;
        }

        if (phase == Phase.LOOK_DOWN) {
            if (!ItemUtil.isHealingSplash(p.getMainHandStack())) {
                cancelSequence();
                return;
            }
            phaseTick++;
            int duration = lookDownTicks.getInt();
            float progress = Math.min(1.0f, phaseTick / (float) duration);
            // Smoothstep creates a hesitant start and a gentler finish.
            float eased = progress * progress * (3f - 2f * progress);
            float wobble = (float) (Math.sin(phaseTick * 1.8) * 1.7 * Math.sin(Math.PI * progress));
            p.setPitch(originalPitch + (90f - originalPitch) * eased + wobble);
            if (phaseTick < duration) return;
            p.setPitch(90f);
            // Recheck right before throwing; only splash instant-health potions qualify.
            if (!ItemUtil.isHealingSplash(p.getMainHandStack())) {
                cancelSequence();
                return;
            }
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            p.swingHand(Hand.MAIN_HAND);
            lastThrow = System.currentTimeMillis();
            thrown++;
            phase = Phase.LOOK_BACK;
            phaseTick = 0;
            return;
        }
        phaseTick++;
        int duration = lookBackTicks.getInt();
        float progress = Math.min(1.0f, phaseTick / (float) duration);
        float eased = progress * progress * (3f - 2f * progress);
        float wobble = (float) (Math.sin(phaseTick * 1.5) * 1.1 * Math.sin(Math.PI * progress));
        p.setPitch(90f + (originalPitch - 90f) * eased + wobble);
        if (phaseTick >= duration) finishSequence();
    }

    private void finishSequence() {
        if (mc.player != null) {
            mc.player.setPitch(originalPitch);
            // Restore the hotbar slot after returning to the previous view angle.
            if (originalSlot >= 0 && InvUtil.selected() == activeSlot) InvUtil.select(originalSlot);
        }
        clearSequence();
    }

    private void cancelSequence() {
        if (phase != Phase.IDLE && mc.player != null) {
            mc.player.setPitch(originalPitch);
            if (originalSlot >= 0 && InvUtil.selected() == activeSlot) InvUtil.select(originalSlot);
        }
        clearSequence();
    }

    private void clearSequence() {
        phase = Phase.IDLE;
        originalSlot = -1;
        activeSlot = -1;
        phaseTick = 0;
        ActionManager.release(Channel.HOTBAR, this);
        ActionManager.release(Channel.ROTATION, this);
    }

    private void refillDedicatedSlot() {
        if (mc.currentScreen != null || mc.player == null) return;
        if (System.currentTimeMillis() - lastRestock < restockDelay.get()) return;
        int target = potionSlot.getInt() - 1;
        ItemStack existing = InvUtil.stack(target);
        // Never displace a different item (or a valid potion already in this slot).
        if (!existing.isEmpty()) return;
        int source = InvUtil.findMainInventory(ItemUtil::isHealingSplash);
        if (source < 0) return;
        if (!ActionManager.isFree(Channel.INVENTORY, this) || !ActionManager.isFree(Channel.HOTBAR, this)) return;
        if (!ActionManager.claim(Channel.INVENTORY, this)) return;
        if (!ActionManager.claim(Channel.HOTBAR, this)) return;
        InvUtil.swapWithHotbar(source, target);
        lastRestock = System.currentTimeMillis();
    }

    private boolean potionAlreadyAtFeet(ClientPlayerEntity p) {
        if (mc.world == null) return false;
        List<Entity> list = mc.world.getOtherEntities(p, p.getBoundingBox().expand(3.0, 4.0, 3.0),
                e -> e.getType() == EntityType.SPLASH_POTION);
        return !list.isEmpty();
    }

    public int potionCount() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = InvUtil.stack(i);
            if (ItemUtil.isHealingSplash(stack)) count += stack.getCount();
        }
        return count;
    }

    @Override public String hudInfo() { return mc.player == null ? null : String.valueOf(potionCount()); }
    @Override public void onDisable() { cancelSequence(); }
    @Override public void onWorldChange() { cancelSequence(); lastThrow = 0; lastRestock = 0; }
}
