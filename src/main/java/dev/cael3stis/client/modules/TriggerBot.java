package dev.cael3stis.client.modules;

import dev.cael3stis.client.Cael3stisClient;
import dev.cael3stis.client.ActionCoordinator;
import dev.cael3stis.client.GameContext;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_3489;
import net.minecraft.class_3966;

/** A legitimate-range click action, not a packet spoof or reach modification. */
public final class TriggerBot extends Module {
    private final Setting.Number delayMs = number("Attack delay (ms)", 110, 0, 500, 10);
    private final Setting.Number cooldown = number("Cooldown (%)", 95, 1, 100, 1);
    private final Setting.Toggle allowFist = toggle("Allow fist attacks", false);
    private final Setting.Toggle onlyCritical = toggle("Critical hits only", false);

    public TriggerBot() { super("TriggerBot", "Combat", "Attack players beneath the crosshair at normal reach."); }

    @Override public void tick(GameContext c) {
        if (c.client.field_1755 != null || c.client.field_1761 == null || !ready(0)) return;
        if (!(c.client.field_1765 instanceof class_3966 hit)) return;
        if (!(hit.method_17782() instanceof class_1657 target) || !Cael3stisClient.isEnemy(c.player, target)) return;
        if (!c.player.method_6057(target) || !c.player.method_76729(target.method_5829(), 0.0)) return;
        boolean weapon = c.player.method_6047().method_31573(class_3489.field_42611) || c.player.method_6047().method_31573(class_3489.field_42612);
        if (!weapon && !(allowFist.value && c.player.method_6047().method_7960())) return;
        if (c.player.method_7261(0) * 100.0f < cooldown.value) return;
        if (onlyCritical.value && (c.player.method_24828() || c.player.method_5799() || c.player.method_6101())) return;
        if (!ActionCoordinator.claim(name)) return;
        c.client.field_1761.method_2918(c.player, target);
        c.player.method_6104(class_1268.field_5808);
        delay(delayMs.value);
    }
}
