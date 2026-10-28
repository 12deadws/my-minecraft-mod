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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1657;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_746;

/** Builds an obsidian cage around the selected enemy, within vanilla placement reach. Never targets friends. */
public class AutoTrap extends Module {
    private final NumberSetting trapRange = add(new NumberSetting("Trap Range", "Only trap enemies within this distance.", 5.0, 2.0, 6.0, 0.5, " m"));
    private final NumberSetting speed = add(new NumberSetting("Placement Speed", "Blocks placed per second.", 10, 1, 20, 1, "/s"));
    private final EnumSetting priority = add(new EnumSetting("Target Priority", "Which enemy to trap.", "Closest", "Closest", "Lowest Health"));
    private final EnumSetting mode = add(new EnumSetting("Trap Mode", "Feet: ring at foot level. Body: feet + head ring. Full: body + top cap.", "Full", "Feet", "Body", "Full"));
    private final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch", "Switch to obsidian in the hotbar and restore afterwards.", true));

    private long lastPlace = 0;
    private class_1657 target;

    public AutoTrap() {
        super("Auto Trap", "Places obsidian around a nearby enemy. Stops when the target escapes or leaves range.", Category.CRYSTAL, 75);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1761 == null) return;

        if (target == null || !Targets.isValidEnemy(target) || p.method_5739(target) > trapRange.get() + 1.0) {
            target = pickTarget(p);
        }
        if (target == null) return;

        if (System.currentTimeMillis() - lastPlace < 1000L / speed.getInt()) return;
        if (InvUtil.findHotbarItem(class_1802.field_8281) < 0 && !p.method_6047().method_31574(class_1802.field_8281)) return;
        if (!ActionManager.isFree(Channel.HOTBAR, this)) return;

        double reach = Targets.blockReach(6.0);
        for (class_2338 pos : trapPositions(target.method_24515())) {
            if (!BlockUtil.canPlaceBlockAt(pos)) continue;
            if (!BlockUtil.isInRange(pos, reach)) continue;
            if (!ActionManager.claim(Channel.HOTBAR, this)) return;
            if (BlockUtil.placeBlock(pos, s -> s.method_31574(class_1802.field_8281), autoSwitch.get(), true)) {
                lastPlace = System.currentTimeMillis();
                return;
            }
        }
    }

    private class_1657 pickTarget(class_746 p) {
        class_1657 best = null;
        double bestScore = Double.MAX_VALUE;
        for (class_1657 e : Targets.enemies(trapRange.get())) {
            double score = priority.is("Lowest Health") ? e.method_6032() + e.method_6067() : p.method_5739(e);
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Positions bottom-up so every block has a support when placed. */
    private List<class_2338> trapPositions(class_2338 feet) {
        List<class_2338> out = new ArrayList<>();
        // Support below the horizontal ring blocks is handled by the placement finder (it uses any neighbour).
        for (class_2350 d : class_2350.class_2353.field_11062) out.add(feet.method_10093(d));
        if (!mode.is("Feet")) {
            for (class_2350 d : class_2350.class_2353.field_11062) out.add(feet.method_10084().method_10093(d));
        }
        if (mode.is("Full")) {
            out.add(feet.method_10086(2));
        }
        out.removeIf(pos -> mc.field_1687.method_8320(pos).method_27852(class_2246.field_10540) || mc.field_1687.method_8320(pos).method_27852(class_2246.field_9987));
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
