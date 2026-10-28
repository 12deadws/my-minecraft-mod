package dev.cael3stis.client.modules;

import dev.cael3stis.client.ActionCoordinator;
import dev.cael3stis.client.GameContext;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

/** Equips offhand totems using normal inventory SWAP clicks (no inventory packet tricks). */
public final class AutoTotem extends Module {
    private final Setting.Number health = number("Health threshold (HP)", 12, 1, 20, 1);
    private final Setting.Number delayMs = number("Refill delay (ms)", 100, 0, 800, 25);
    private final Setting.Number jitterMs = number("Random extra delay (ms)", 50, 0, 500, 25);
    private final Setting.Toggle always = toggle("Always totem", false);
    private final Setting.Toggle smart = toggle("Smart combat mode", true);
    private final Setting.Toggle restore = toggle("Restore previous offhand item", false);
    private long nextRefill;
    private boolean wasDangerous;
    private int switchedFrom = -1;
    private class_1799 oldOffhand = class_1799.field_8037;

    public AutoTotem() { super("Auto Totem", "Player", "Equip a totem from inventory when at risk."); }

    @Override public void reset() {
        super.reset(); nextRefill = 0; wasDangerous = false; switchedFrom = -1; oldOffhand = class_1799.field_8037;
    }

    @Override public void tick(GameContext c) {
        if (c.client.field_1761 == null || c.client.field_1755 != null) return;
        boolean danger = always.value || c.player.method_6032() + c.player.method_6067() <= health.value
                || (smart.value && c.player.field_6235 > 0 && c.player.method_6032() <= health.value + 4);
        if (danger && !wasDangerous) {
            nextRefill = System.currentTimeMillis() + randomDelay();
        }
        wasDangerous = danger;
        if (!danger && restore.value && switchedFrom >= 0 && c.player.method_6079().method_31574(class_1802.field_8288)) {
            // Only reverse our own swap if the source slot still holds the old offhand item.
            if (class_1799.method_31577(c.player.method_31548().method_5438(switchedFrom), oldOffhand)
                    && ActionCoordinator.claim(name)) {
                swapOffhand(c, switchedFrom);
                switchedFrom = -1;
            }
            return;
        }
        if (!danger || c.player.method_6079().method_31574(class_1802.field_8288)
                || System.currentTimeMillis() < nextRefill || ActionCoordinator.isBusy()) return;
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            if (c.player.method_31548().method_5438(i).method_31574(class_1802.field_8288)) { slot = i; break; }
        }
        if (slot < 0 || !ActionCoordinator.claim(name)) return;
        oldOffhand = c.player.method_6079().method_7972();
        switchedFrom = slot;
        swapOffhand(c, slot);
        nextRefill = System.currentTimeMillis() + randomDelay();
    }

    private int randomDelay() {
        return delayMs.value + (jitterMs.value == 0 ? 0 : ThreadLocalRandom.current().nextInt(jitterMs.value + 1));
    }

    private void swapOffhand(GameContext c, int inventorySlot) {
        // Player screen handler: main inventory 9-35; hotbar 36-44.
        int screenSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        c.client.field_1761.method_2906(c.player.field_7498.field_7763,
                screenSlot, 40, class_1713.field_7791, c.player);
    }
}
