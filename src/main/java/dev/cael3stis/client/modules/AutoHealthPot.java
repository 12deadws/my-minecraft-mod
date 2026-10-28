package dev.cael3stis.client.modules;

import dev.cael3stis.client.ActionCoordinator;
import dev.cael3stis.client.GameContext;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import net.minecraft.class_1268;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1844;
import net.minecraft.class_310;
import net.minecraft.class_9334;

/** Uses only pure instant-health splash potions, preferring Healing II. */
public final class AutoHealthPot extends Module {
    private final Setting.Number health = number("Activation health (HP)", 8, 1, 20, 1);
    private final Setting.Number cooldown = number("Cooldown (ms)", 1000, 250, 5000, 250);
    private int pendingRestoreSlot = -1;
    private float pendingPitch;
    private boolean pendingRestore;

    public AutoHealthPot() { super("Auto Health Pot", "Player", "Use only splash instant-health potions at low health."); }

    @Override public void reset() {
        if (pendingRestore) {
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 != null) {
                mc.field_1724.method_31548().method_61496(pendingRestoreSlot);
                mc.field_1724.method_36457(pendingPitch);
            }
        }
        super.reset(); pendingRestore = false; pendingRestoreSlot = -1;
    }

    @Override public void tick(GameContext c) {
        if (pendingRestore) {
            c.player.method_31548().method_61496(pendingRestoreSlot);
            c.player.method_36457(pendingPitch);
            pendingRestore = false;
            pendingRestoreSlot = -1;
            return;
        }
        if (c.client.field_1761 == null || c.client.field_1755 != null || !ready(0)
                || c.player.method_6032() > health.value || c.player.method_6115()
                || c.player.method_6047().method_31574(class_1802.field_8463)
                || c.player.method_6047().method_31574(class_1802.field_8367)
                || ActionCoordinator.isBusy()) return;

        int selected = -1, bestAmp = -1;
        for (int i = 0; i < 9; i++) {
            class_1799 stack = c.player.method_31548().method_5438(i);
            int amp = healingAmplifier(stack);
            if (amp > bestAmp) { bestAmp = amp; selected = i; }
        }
        if (selected < 0 || !ActionCoordinator.claim(name)) return;
        int previousSlot = c.player.method_31548().method_67532();
        float previousPitch = c.player.method_36455();
        c.player.method_31548().method_61496(selected);
        // Revalidate after the selection and before use; rejects any non-healing potion.
        if (healingAmplifier(c.player.method_6047()) < 0) {
            c.player.method_31548().method_61496(previousSlot);
            return;
        }
        c.player.method_36457(90.0f); // face downward for the splash throw
        c.client.field_1761.method_2919(c.player, class_1268.field_5808);
        pendingRestoreSlot = previousSlot;
        pendingPitch = previousPitch;
        pendingRestore = true;
        delay(cooldown.value);
    }

    /** -1 means reject, 0 means Healing I, 1 means Healing II. */
    private static int healingAmplifier(class_1799 stack) {
        if (!stack.method_31574(class_1802.field_8436)) return -1;
        class_1844 contents = stack.method_58694(class_9334.field_49651);
        if (contents == null) return -1;
        int amp = -1, effects = 0;
        for (class_1293 effect : contents.method_57397()) {
            effects++;
            if (!effect.method_5579().equals(class_1294.field_5915) || effect.method_5578() > 1) return -1;
            amp = effect.method_5578();
        }
        return effects == 1 ? amp : -1;
    }
}
