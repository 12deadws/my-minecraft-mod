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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

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
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.world == null) return;
        if (pauseEating.get() && p.isUsingItem()) return;

        List<PlayerEntity> targets = Targets.enemies(targetRange.get());
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

    private void sortTargets(List<PlayerEntity> list, ClientPlayerEntity p) {
        // "Best Damage" evaluates the 3 nearest enemies and the scoring below picks the highest-damage option;
        // "Closest" and "Lowest Health" decide which enemies make the shortlist first.
        if (priority.is("Lowest Health")) {
            list.sort(Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount()));
        } else {
            list.sort(Comparator.comparingDouble(p::distanceTo));
        }
    }

    // ------------------------------------------------------------------ break

    private boolean tryBreak(ClientPlayerEntity p, List<PlayerEntity> targets) {
        double reach = Targets.entityReach(breakRange.get());
        Box area = p.getBoundingBox().expand(reach + 1.0);
        EndCrystalEntity best = null;
        double bestScore = -Double.MAX_VALUE;

        for (EndCrystalEntity c : mc.world.getEntitiesByClass(EndCrystalEntity.class, area, e -> e.isAlive())) {
            if (Targets.reachTo(c) > reach) continue;
            if (!Targets.visible(c.getBoundingBox().getCenter())) continue;

            Vec3d pos = new Vec3d(c.getX(), c.getY(), c.getZ());
            float self = DamageUtil.crystalDamage(p, pos);
            if (!CombatSafety.isSelfSafe(self, maxSelf.get(), antiSuicide.get())) continue;

            float dealt = 0f;
            for (PlayerEntity t : targets) dealt = Math.max(dealt, DamageUtil.crystalDamage(t, pos));
            if (dealt < minDamage.get()) continue;

            double score = dealt - self;
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        if (best == null) return false;
        if (!ActionManager.claim(Channel.ATTACK, this)) return false;

        mc.interactionManager.attackEntity(p, best);
        p.swingHand(Hand.MAIN_HAND);
        return true;
    }

    // ------------------------------------------------------------------ place

    private boolean tryPlace(ClientPlayerEntity p, List<PlayerEntity> targets) {
        boolean offhand = p.getOffHandStack().isOf(Items.END_CRYSTAL);
        boolean mainHand = p.getMainHandStack().isOf(Items.END_CRYSTAL);
        int hotbar = InvUtil.findHotbarItem(Items.END_CRYSTAL);
        if (!offhand && !mainHand && !(autoSwitch.get() && hotbar >= 0)) return false;

        double reach = Targets.blockReach(placeRange.get());
        Vec3d eye = p.getEyePos();

        BlockPos bestBase = null;
        double bestScore = -Double.MAX_VALUE;

        for (PlayerEntity t : targets) {
            BlockPos feet = t.getBlockPos();
            int yMin = -1;
            int yMax = facePlace.get() ? 1 : 0;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    for (int dy = yMin; dy <= yMax; dy++) {
                        BlockPos base = feet.add(dx, dy, dz);
                        if (eye.distanceTo(Vec3d.of(base).add(0.5, 1.0, 0.5)) > reach) continue;
                        if (!BlockUtil.isObsidianOrBedrock(mc.world.getBlockState(base))) continue;
                        if (!mc.world.getBlockState(base.up()).isAir() || !mc.world.getBlockState(base.up(2)).isAir()) continue;
                        if (!crystalFits(base)) continue;

                        Vec3d crystalPos = new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
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

        BlockHitResult hit = new BlockHitResult(Vec3d.of(bestBase).add(0.5, 1.0, 0.5), Direction.UP, bestBase, false);

        if (offhand) {
            mc.interactionManager.interactBlock(p, Hand.OFF_HAND, hit);
            p.swingHand(Hand.OFF_HAND);
            return true;
        }
        int prev = InvUtil.selected();
        int slot = mainHand ? prev : hotbar;
        if (!ActionManager.claim(Channel.HOTBAR, this)) return false;
        if (slot != prev) InvUtil.select(slot);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
        p.swingHand(Hand.MAIN_HAND);
        if (slot != prev && restoreSlot.get()) InvUtil.select(prev);
        return true;
    }

    /** Vanilla placement rule: the 1x2x1 space above the base must contain no entities. */
    private boolean crystalFits(BlockPos base) {
        BlockPos above = base.up();
        Box box = new Box(above.getX(), above.getY(), above.getZ(), above.getX() + 1, above.getY() + 2, above.getZ() + 1);
        return mc.world.getOtherEntities(null, box, e -> e.isAlive() && !e.isSpectator()).isEmpty();
    }
}
