package dev.cael3stis.client.modules;

import dev.cael3stis.client.ActionCoordinator;
import dev.cael3stis.client.GameContext;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import net.minecraft.class_1713;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

/** Swaps fuller inventory stacks into hotbar slots through normal player inventory interactions. */
public final class AutoRefill extends Module {
    private final Setting.Number threshold = number("Minimum hotbar count", 12, 0, 63, 1);
    private final Setting.Number delayMs = number("Refill delay (ms)", 500, 100, 3000, 100);
    private final Setting.Toggle crystals = toggle("End crystals", true);
    private final Setting.Toggle obsidian = toggle("Obsidian", true);
    private final Setting.Toggle apples = toggle("Golden apples", true);
    private final Setting.Toggle totems = toggle("Totems", true);
    private final Setting.Toggle anchors = toggle("Respawn anchors", true);
    private final Setting.Toggle glowstone = toggle("Glowstone", true);

    public AutoRefill() { super("Auto Refill", "Player", "Swap fuller supplies into matching hotbar slots."); }

    @Override public void tick(GameContext c) {
        if (c.client.field_1755 != null || c.client.field_1761 == null || !ready(0)
                || ActionCoordinator.isBusy() || c.player.field_6235 > 0) return;
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            class_1799 current = c.player.method_31548().method_5438(hotbar);
            if (current.method_7960() || current.method_7947() > threshold.value || !enabledItem(current.method_7909())) continue;
            int best = -1, count = current.method_7947();
            for (int inv = 9; inv < 36; inv++) {
                class_1799 candidate = c.player.method_31548().method_5438(inv);
                if (class_1799.method_31577(current, candidate) && candidate.method_7947() > count) {
                    best = inv; count = candidate.method_7947();
                }
            }
            if (best < 0 || !ActionCoordinator.claim(name)) return;
            c.client.field_1761.method_2906(c.player.field_7498.field_7763,
                    best, hotbar, class_1713.field_7791, c.player);
            delay(delayMs.value);
            return; // exactly one normal inventory action each delay
        }
    }

    private boolean enabledItem(class_1792 item) {
        return (item == class_1802.field_8301 && crystals.value)
                || (item == class_1802.field_8281 && obsidian.value)
                || (item == class_1802.field_8463 && apples.value)
                || (item == class_1802.field_8367 && apples.value)
                || (item == class_1802.field_8288 && totems.value)
                || (item == class_1802.field_23141 && anchors.value)
                || (item == class_1802.field_8801 && glowstone.value);
    }
}
