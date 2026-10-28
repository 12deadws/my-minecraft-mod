package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.BlockUtil;
import com.caelestis.client.util.InvUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Places obsidian on the four horizontal sides of the block you stand in, using normal block placement. */
public class Surround extends Module {
    private final NumberSetting placeDelay = add(new NumberSetting("Placement Delay", "Time between two placements.", 50, 0, 500, 10, " ms"));
    private final BoolSetting autoDisable = add(new BoolSetting("Auto Disable", "Turn off once the surround is complete.", false));
    private final BoolSetting center = add(new BoolSetting("Center Player", "Walk to the middle of your block first (normal movement speed).", true));
    private final BoolSetting restoreSlot = add(new BoolSetting("Restore Previous Slot", "Switch back to your previous hotbar slot after placing.", true));
    private final BoolSetting onlyStationary = add(new BoolSetting("Only When Stationary", "Only place while you are not moving.", false));
    private final BoolSetting disableOnMove = add(new BoolSetting("Disable When Moving", "Turn off if you leave the block you started in.", false));

    private long lastPlace = 0;
    private BlockPos home;
    private boolean complete = false;

    public Surround() {
        super("Surround", "Places obsidian around your feet.", Category.CRYSTAL, 70);
    }

    @Override
    public void onEnable() {
        home = mc.player == null ? null : mc.player.getBlockPos();
        complete = false;
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        BlockPos feet = p.getBlockPos();
        if (home == null) home = feet;
        if (disableOnMove.get() && !feet.equals(home)) {
            setEnabled(false);
            return;
        }
        double speedSq = p.getVelocity().x * p.getVelocity().x + p.getVelocity().z * p.getVelocity().z;
        if (onlyStationary.get() && speedSq > 0.0004) return;

        if (center.get() && p.isOnGround() && !complete) nudgeToCenter(p, feet);

        List<BlockPos> missing = missing(feet);
        if (missing.isEmpty()) {
            if (!complete) {
                complete = true;
                if (autoDisable.get()) setEnabled(false);
            }
            return;
        }
        complete = false;

        if (System.currentTimeMillis() - lastPlace < (long) placeDelay.get()) return;
        if (InvUtil.findHotbarItem(Items.OBSIDIAN) < 0) return;
        if (!ActionManager.isFree(Channel.HOTBAR, this)) return;

        for (BlockPos pos : missing) {
            if (!ActionManager.claim(Channel.HOTBAR, this)) return;
            if (BlockUtil.placeBlock(pos, s -> s.isOf(Items.OBSIDIAN), true, restoreSlot.get())) {
                lastPlace = System.currentTimeMillis();
                return; // one block per delay window
            }
        }
    }

    /** Side positions that still need a block and can legally receive one. */
    private List<BlockPos> missing(BlockPos feet) {
        List<BlockPos> out = new ArrayList<>();
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos pos = feet.offset(d);
            if (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) continue;
            if (!BlockUtil.canPlaceBlockAt(pos)) continue; // occupied by something we may not place into
            out.add(pos);
        }
        return out;
    }

    /** Gentle horizontal velocity toward the block centre; stays within normal walking speed. */
    private void nudgeToCenter(ClientPlayerEntity p, BlockPos feet) {
        double cx = feet.getX() + 0.5, cz = feet.getZ() + 0.5;
        double dx = cx - p.getX(), dz = cz - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.08) return;
        double step = Math.min(dist, 0.1);
        Vec3d v = p.getVelocity();
        p.setVelocity(dx / dist * step, v.y, dz / dist * step);
    }

    @Override
    public void onWorldChange() {
        home = null;
        complete = false;
    }
}
