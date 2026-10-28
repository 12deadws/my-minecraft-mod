package com.caelestis.client.modules;

import com.caelestis.client.module.ActionManager;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.ItemUtil;
import com.caelestis.client.util.Targets;
import net.minecraft.class_1268;
import net.minecraft.class_1294;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_239;
import net.minecraft.class_3966;
import net.minecraft.class_746;

public class TriggerBot extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Reaction delay after a target is under the crosshair and your cooldown is ready.", 100, 0, 500, 10, " ms"));
    private final BoolSetting critOnly = add(new BoolSetting("Critical Hits Only", "Only attack while a vanilla critical hit is possible (falling, not sprinting).", false));
    private final BoolSetting allowFist = add(new BoolSetting("Allow Fist Attacks", "Also attack with an empty hand. Off by default.", false));

    private long aimedSince = 0;
    private class_1657 lastTarget;

    public TriggerBot() {
        super("TriggerBot", "Attacks the player under your crosshair with real attacks, respecting cooldown, reach and line of sight.", Category.COMBAT, 60);
    }

    @Override
    public void onTick() {
        class_746 p = mc.field_1724;
        if (p == null || mc.field_1761 == null) return;

        class_239 hr = mc.field_1765;
        if (!(hr instanceof class_3966 ehr)) {
            aimedSince = 0;
            return;
        }
        class_1297 e = ehr.method_17782();
        if (!(e instanceof class_1657 target) || !Targets.isValidEnemy(target)) {
            aimedSince = 0;
            return;
        }
        if (target != lastTarget) {
            aimedSince = 0;
            lastTarget = target;
        }
        if (!weaponAllowed(p.method_6047())) {
            aimedSince = 0;
            return;
        }
        if (p.method_6115()) return;

        // Real reach + line of sight (crosshair target is already vanilla-validated; double check).
        if (Targets.reachTo(target) > p.method_55755()) return;
        if (!Targets.canSee(target)) return;

        long now = System.currentTimeMillis();
        if (aimedSince == 0) aimedSince = now;

        if (p.method_7261(0.0f) < 0.95f) return;
        if (now - aimedSince < (long) delay.get()) return;
        if (critOnly.get() && !canCrit(p)) return;

        // Do not fight over the attack action with a higher-priority module this tick.
        if (!ActionManager.claim(ActionManager.Channel.ATTACK, this)) return;

        Targets.prefer(target);
        mc.field_1761.method_2918(p, target);
        p.method_6104(class_1268.field_5808);
        aimedSince = now; // next swing waits for cooldown + delay again
    }

    private boolean weaponAllowed(class_1799 s) {
        if (s.method_7960()) return allowFist.get();
        return ItemUtil.isSword(s) || ItemUtil.isAxe(s);
    }

    /** Vanilla critical-hit conditions. */
    private boolean canCrit(class_746 p) {
        return !p.method_24828() && p.field_6017 > 0.0 && !p.method_6101() && !p.method_5799()
                && !p.method_6059(class_1294.field_5919) && !p.method_5765() && !p.method_5624();
    }

    @Override
    public void onDisable() {
        aimedSince = 0;
        lastTarget = null;
    }

    @Override
    public void onWorldChange() {
        onDisable();
    }
}
