package dev.cael3stis.client.modules;

import dev.cael3stis.client.Cael3stisClient;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import net.minecraft.class_1657;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_332;

/** Lightweight, opt-in top-left information overlay; does not expose unseen server information. */
public final class InfoHud extends Module {
    private static final int GOLD = 0xFFE4B95E;
    private static final int LIGHT = 0xFFFAF7EE;
    private final Setting.Toggle moduleList = toggle("List active modules", true);
    private final Setting.Toggle totemCount = toggle("Show totem count", true);
    private final Setting.Toggle playerList = toggle("Nearby player list", false);
    private final Setting.Number playerRange = number("Player range", 24, 4, 64, 4);

    public InfoHud() { super("Info HUD", "Render", "Show active modules, totems and nearby players."); }

    public void render(class_332 ctx) {
        class_310 c = class_310.method_1551();
        if (!enabled || c.field_1724 == null || c.field_1687 == null || c.field_1690.field_1842) return;
        int x = 8, y = 8;
        ctx.method_25294(4, 4, 160, 21, 0xBC171D28);
        ctx.method_25303(c.field_1772, "Cael3stis  v" + Cael3stisClient.VERSION, x, y, GOLD);
        y += 18;
        if (totemCount.value) {
            int count = c.field_1724.method_6079().method_31574(class_1802.field_8288) ? c.field_1724.method_6079().method_7947() : 0;
            for (int i = 0; i < 36; i++) if (c.field_1724.method_31548().method_5438(i).method_31574(class_1802.field_8288)) {
                count += c.field_1724.method_31548().method_5438(i).method_7947();
            }
            ctx.method_25303(c.field_1772, "Totems: " + count, x, y, LIGHT);
            y += 12;
        }
        if (moduleList.value) {
            for (Module module : Cael3stisClient.modules()) if (module.enabled && module != this) {
                ctx.method_25303(c.field_1772, module.name, x, y, GOLD);
                y += 12;
            }
        }
        if (playerList.value) {
            y += 5;
            ctx.method_25303(c.field_1772, "Nearby players", x, y, GOLD);
            y += 12;
            int shown = 0;
            for (class_1657 other : c.field_1687.method_18456()) {
                if (!Cael3stisClient.isEnemy(c.field_1724, other)) continue;
                double distance = c.field_1724.method_5739(other);
                if (distance > playerRange.value) continue;
                ctx.method_25303(c.field_1772, other.method_5477().getString() + "  " + (int) distance + "m", x, y, LIGHT);
                y += 12;
                if (++shown >= 10) break;
            }
        }
    }
}
