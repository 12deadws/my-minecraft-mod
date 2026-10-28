package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.ActionManager.Channel;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.BlockUtil;
import com.caelestis.client.util.InvUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_746;

/** Places obsidian on the four horizontal sides of the block you stand in, using normal block placement. */
public class Surround extends Module {
    private final NumberSetting placeDelay = add(new NumberSetting("Placement Delay", "Time between two placements.", 50, 0, 500, 10, " ms"));
    private final BoolSetting autoDisable = add(new BoolSetting("Auto Disable", "Turn off once the surround is complete.", false));
    private final BoolSetting center = add(new BoolSetting("Center Player", "Walk to the middle of your block first (normal movement speed).", true));
    private final BoolSetting restoreSlot = add(new BoolSetting("Restore Previous Slot", "Switch back to your previous hotbar slot after placing.", true));
    private final BoolSetting onlyStationary = add(new BoolSetting("Only When Stationary", "Only place while you are not moving.", false));
    private final BoolSetting disableOnMove = add(new BoolSetting("Disable When Moving", "Turn off if you leave the block you started in.", false));

    private long lastPlace = 0;
    private class_2338 home;
    private boolean complete = false;

    public Surround() {
        super("Surround", "Places obsidian around your feet.", Category.CRYSTAL, 70);
    }

    @Override
    public void onEnable() {
        home = mc.field_1724 == null ? null : mc.field_1724.method_24515();
        complete = false;
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1761 == null) return;

        class_2338 feet = p.method_24515();
        if (home == null) home = feet;
        if (disableOnMove.get() && !feet.equals(home)) {
            setEnabled(false);
            return;
        }
        double speedSq = p.method_18798().field_1352 * p.method_18798().field_1352 + p.method_18798().field_1350 * p.method_18798().field_1350;
        if (onlyStationary.get() && speedSq > 0.0004) return;

        if (center.get() && p.method_24828() && !complete) nudgeToCenter(p, feet);

        List<class_2338> missing = missing(feet);
        if (missing.isEmpty()) {
            if (!complete) {
                complete = true;
                if (autoDisable.get()) setEnabled(false);
            }
            return;
        }
        complete = false;

        if (System.currentTimeMillis() - lastPlace < (long) placeDelay.get()) return;
        if (InvUtil.findHotbarItem(class_1802.field_8281) < 0) return;
        if (!ActionManager.isFree(Channel.HOTBAR, this)) return;

        for (class_2338 pos : missing) {
            if (!ActionManager.claim(Channel.HOTBAR, this)) return;
            if (BlockUtil.placeBlock(pos, s -> s.method_31574(class_1802.field_8281), true, restoreSlot.get())) {
                lastPlace = System.currentTimeMillis();
                return; // one block per delay window
            }
        }
    }

    /** Side positions that still need a block and can legally receive one. */
    private List<class_2338> missing(class_2338 feet) {
        List<class_2338> out = new ArrayList<>();
        for (class_2350 d : class_2350.class_2353.field_11062) {
            class_2338 pos = feet.method_10093(d);
            if (mc.field_1687.method_8320(pos).method_27852(class_2246.field_10540) || mc.field_1687.method_8320(pos).method_27852(class_2246.field_9987)) continue;
            if (!BlockUtil.canPlaceBlockAt(pos)) continue; // occupied by something we may not place into
            out.add(pos);
        }
        return out;
    }

    /** Gentle horizontal velocity toward the block centre; stays within normal walking speed. */
    private void nudgeToCenter(class_746 p, class_2338 feet) {
        double cx = feet.method_10263() + 0.5, cz = feet.method_10260() + 0.5;
        double dx = cx - p.method_23317(), dz = cz - p.method_23321();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.08) return;
        double step = Math.min(dist, 0.1);
        class_243 v = p.method_18798();
        p.method_18800(dx / dist * step, v.field_1351, dz / dist * step);
    }

    @Override
    public void onWorldChange() {
        home = null;
        complete = false;
    }
}
