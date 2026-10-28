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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

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

    private BlockPos anchorPos;
    private long stepAt = 0;
    private long startedAt = 0;
    private boolean inNether = false;

    public AnchorAura() {
        super("Anchor Aura", "Explodes Respawn Anchors on enemies (Overworld / End only, never in the Nether).", Category.CRYSTAL, 55);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) return;

        // Hard dimension guard: anchors are respawn points in the Nether and do not explode there.
        inNether = mc.world.getRegistryKey() == World.NETHER;
        if (inNether) {
            anchorPos = null;
            return;
        }

        List<PlayerEntity> targets = Targets.enemies(10.0);
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

        BlockState state = mc.world.getBlockState(anchorPos);
        if (!BlockUtil.isInRange(anchorPos, reach + 0.5)) {
            anchorPos = null;
            return;
        }

        if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges == 0) {
                if (charge(p)) stepAt = System.currentTimeMillis() + (long) activateDelay.get();
                return;
            }
            detonate(p, targets);
            return;
        }

        if (state.isReplaceable()) {
            if (place(p)) stepAt = System.currentTimeMillis() + (long) placeDelay.get();
            else anchorPos = null;
        } else {
            anchorPos = null; // something else is there now
        }
    }

    private BlockPos chooseSpot(ClientPlayerEntity p, List<PlayerEntity> targets, double reach) {
        if (InvUtil.findHotbarItem(Items.RESPAWN_ANCHOR) < 0 && !p.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return null;
        if (InvUtil.findHotbarItem(Items.GLOWSTONE) < 0) return null;
        // Offhand glowstone would be consumed instead of detonating, so refuse in that case.
        if (p.getOffHandStack().isOf(Items.GLOWSTONE)) return null;

        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;
        for (PlayerEntity t : targets) {
            BlockPos feet = t.getBlockPos();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        BlockPos pos = feet.add(dx, dy, dz);
                        if (!BlockUtil.isInRange(pos, reach)) continue;
                        if (!mc.world.getBlockState(pos).isReplaceable() || !BlockUtil.canPlaceBlockAt(pos)) continue;
                        if (BlockUtil.findPlacement(pos, reach) == null) continue;

                        Vec3d center = Vec3d.ofCenter(pos);
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

    private boolean place(ClientPlayerEntity p) {
        if (!ActionManager.claim(Channel.HOTBAR, this)) return false;
        return BlockUtil.placeBlock(anchorPos, s -> s.isOf(Items.RESPAWN_ANCHOR), autoSwitch.get(), restoreSlot.get());
    }

    private boolean charge(ClientPlayerEntity p) {
        int slot = p.getMainHandStack().isOf(Items.GLOWSTONE) ? InvUtil.selected() : InvUtil.findHotbarItem(Items.GLOWSTONE);
        if (slot < 0) return false;
        if (slot != InvUtil.selected() && !autoSwitch.get()) return false;
        if (!ActionManager.claim(Channel.HOTBAR, this)) return false;

        int prev = InvUtil.selected();
        InvUtil.select(slot);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, clickOn(anchorPos));
        p.swingHand(Hand.MAIN_HAND);
        if (restoreSlot.get()) InvUtil.select(prev);
        return true;
    }

    private void detonate(ClientPlayerEntity p, List<PlayerEntity> targets) {
        // Re-check the damage model right before detonating (things move).
        Vec3d center = Vec3d.ofCenter(anchorPos);
        float dealt = 0f;
        for (PlayerEntity t : targets) dealt = Math.max(dealt, DamageUtil.explosionDamage(t, center, DamageUtil.ANCHOR_POWER, anchorPos));
        float self = DamageUtil.explosionDamage(p, center, DamageUtil.ANCHOR_POWER, anchorPos);
        if (dealt < minDamage.get() || !CombatSafety.isSelfSafe(self, maxSelf.get(), antiSuicide.get())) {
            anchorPos = null; // unfavourable: walk away from this anchor
            return;
        }
        if (p.getOffHandStack().isOf(Items.GLOWSTONE)) return;

        // Detonation needs a non-glowstone item in the main hand.
        int prev = InvUtil.selected();
        int slot = p.getMainHandStack().isOf(Items.GLOWSTONE) ? nonGlowstoneSlot() : prev;
        if (slot < 0) return;
        if (slot != prev && !autoSwitch.get()) return;
        if (!ActionManager.claim(Channel.HOTBAR, this)) return;

        if (slot != prev) InvUtil.select(slot);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, clickOn(anchorPos));
        p.swingHand(Hand.MAIN_HAND);
        if (slot != prev && restoreSlot.get()) InvUtil.select(prev);
        anchorPos = null;
        stepAt = System.currentTimeMillis() + (long) placeDelay.get();
    }

    private int nonGlowstoneSlot() {
        for (int i = 0; i < 9; i++) {
            if (!InvUtil.stack(i).isOf(Items.GLOWSTONE)) return i;
        }
        return -1;
    }

    private BlockHitResult clickOn(BlockPos pos) {
        return new BlockHitResult(Vec3d.ofCenter(pos).add(0, 0.5, 0), Direction.UP, pos, false);
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
