package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.EnumSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.BlockUtil;
import com.caelestis.client.util.InvUtil;
import com.caelestis.client.util.Targets;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/** Builds an obsidian cage around the selected enemy, within vanilla placement reach. Never targets friends. */
public class AutoTrap extends Module {
    private final NumberSetting trapRange = add(new NumberSetting("Trap Range", "Only trap enemies within this distance.", 5.0, 2.0, 6.0, 0.5, " m"));
    private final NumberSetting speed = add(new NumberSetting("Placement Speed", "Blocks placed per second.", 10, 1, 20, 1, "/s"));
    private final EnumSetting priority = add(new EnumSetting("Target Priority", "Which enemy to trap.", "Closest", "Closest", "Lowest Health"));
    private final EnumSetting mode = add(new EnumSetting("Trap Mode", "Feet: ring at foot level. Body: feet + head ring. Full: body + top cap.", "Full", "Feet", "Body", "Full"));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch", "Switch to obsidian in the hotbar and restore afterwards.", true));

    private long lastPlace = 0;
    private PlayerEntity target;

    public AutoTrap() {
        super("Auto Trap", "Places obsidian around a nearby enemy. Stops when the target escapes or leaves range.", Category.CRYSTAL, 75);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) return;

        if (target == null || !Targets.isValidEnemy(target) || p.distanceTo(target) > trapRange.get() + 1.0) {
            target = pickTarget(p);
        }
        if (target == null) return;

        if (System.currentTimeMillis() - lastPlace < 1000L / speed.getInt()) return;
        if (InvUtil.findHotbarItem(Items.OBSIDIAN) < 0 && !p.getMainHandStack().isOf(Items.OBSIDIAN)) return;
        if (!ActionManager.isFree(Channel.HOTBAR, this)) return;

        double reach = Targets.blockReach(6.0);
        for (BlockPos pos : trapPositions(target.getBlockPos())) {
            if (!BlockUtil.canPlaceBlockAt(pos)) continue;
            if (!BlockUtil.isInRange(pos, reach)) continue;
            if (!ActionManager.claim(Channel.HOTBAR, this)) return;
            if (BlockUtil.placeBlock(pos, s -> s.isOf(Items.OBSIDIAN), autoSwitch.get(), true)) {
                lastPlace = System.currentTimeMillis();
                return;
            }
        }
    }

    private PlayerEntity pickTarget(ClientPlayerEntity p) {
        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (PlayerEntity e : Targets.enemies(trapRange.get())) {
            double score = priority.is("Lowest Health") ? e.getHealth() + e.getAbsorptionAmount() : p.distanceTo(e);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Positions bottom-up so every block has a support when placed. */
    private List<BlockPos> trapPositions(BlockPos feet) {
        List<BlockPos> out = new ArrayList<>();
        // Support below the horizontal ring blocks is handled by the placement finder (it uses any neighbour).
        for (Direction d : Direction.Type.HORIZONTAL) out.add(feet.offset(d));
        if (!mode.is("Feet")) {
            for (Direction d : Direction.Type.HORIZONTAL) out.add(feet.up().offset(d));
        }
        if (mode.is("Full")) {
            out.add(feet.up(2));
        }
        out.removeIf(pos -> mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK));
        return out;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Override
    public void onWorldChange() {
        target = null;
    }
}
