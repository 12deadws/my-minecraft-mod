package com.caelestis.client.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

/** Block placement helpers that follow normal vanilla placement rules and reach. */
public final class BlockUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private BlockUtil() {
    }

    public static boolean isObsidianOrBedrock(BlockState s) {
        return s.isOf(Blocks.OBSIDIAN) || s.isOf(Blocks.BEDROCK);
    }

    /** A block face that can be right-clicked to place against without triggering a GUI/interaction. */
    private static boolean isSafeSupport(BlockPos pos) {
        BlockState s = mc.world.getBlockState(pos);
        if (s.isAir() || s.isReplaceable() || !s.getFluidState().isEmpty()) return false;
        Block b = s.getBlock();
        if (b instanceof BlockWithEntity) return false;
        if (s.isOf(Blocks.CRAFTING_TABLE) || s.isOf(Blocks.NOTE_BLOCK) || s.isOf(Blocks.CAKE)
                || s.isOf(Blocks.RESPAWN_ANCHOR) || s.isOf(Blocks.TNT) || s.isOf(Blocks.CARTOGRAPHY_TABLE)
                || s.isOf(Blocks.FLETCHING_TABLE) || s.isOf(Blocks.SMITHING_TABLE) || s.isOf(Blocks.LOOM)
                || s.isOf(Blocks.STONECUTTER) || s.isOf(Blocks.GRINDSTONE) || s.isOf(Blocks.LECTERN)) return false;
        return s.isFullCube(mc.world, pos);
    }

    /** Can an obsidian block legally be placed at pos (replaceable, no entity in the way)? */
    public static boolean canPlaceBlockAt(BlockPos pos) {
        if (mc.world == null) return false;
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;
        return mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent());
    }

    /** Finds an adjacent support block and returns the click that places a block at pos, or null. */
    public static BlockHitResult findPlacement(BlockPos pos, double maxReach) {
        if (mc.player == null || mc.world == null) return null;
        Vec3d eye = mc.player.getEyePos();
        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            BlockPos support = pos.offset(dir);
            if (!isSafeSupport(support)) continue;
            Direction face = dir.getOpposite();
            Vec3d hit = Vec3d.ofCenter(support).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
            double d = eye.distanceTo(hit);
            if (d > maxReach || d >= bestDist) continue;
            bestDist = d;
            best = new BlockHitResult(hit, face, support, false);
        }
        return best;
    }

    /**
     * Places an item block at pos using a hotbar item matched by pred. Temporarily switches slot if allowed and
     * restores it in the same tick. Returns true if a placement packet was sent.
     */
    public static boolean placeBlock(BlockPos pos, Predicate<ItemStack> itemPred, boolean autoSwitch, boolean restore) {
        if (mc.player == null || mc.interactionManager == null) return false;
        if (!canPlaceBlockAt(pos)) return false;
        BlockHitResult hit = findPlacement(pos, mc.player.getBlockInteractionRange());
        if (hit == null) return false;

        int prev = InvUtil.selected();
        int slot;
        if (itemPred.test(mc.player.getMainHandStack())) {
            slot = prev;
        } else if (autoSwitch) {
            slot = InvUtil.findHotbar(itemPred);
            if (slot < 0) return false;
        } else {
            return false;
        }
        if (slot != prev) InvUtil.select(slot);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (slot != prev && restore) InvUtil.select(prev);
        return true;
    }

    public static boolean isInRange(BlockPos pos, double range) {
        return mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) <= range;
    }
}
