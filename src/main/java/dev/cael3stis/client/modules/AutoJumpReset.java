package dev.cael3stis.client.modules;

import dev.cael3stis.client.GameContext;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import java.util.concurrent.ThreadLocalRandom;

/** Attempts a normal jump after the player's hurt timer is newly set. */
public final class AutoJumpReset extends Module {
    private final Setting.Number chance = number("Jump attempt chance (%)", 100, 0, 100, 5);
    private final Setting.Number reactionDelay = number("Reaction delay (ms)", 35, 0, 200, 5);
    private int previousHurtTicks;
    private long jumpAt = -1;

    public AutoJumpReset() { super("Auto Jump Reset", "Combat", "Attempt a grounded jump following incoming damage."); }

    @Override public void reset() { super.reset(); previousHurtTicks = 0; jumpAt = -1; }

    @Override public void tick(GameContext c) {
        int hurt = c.player.field_6235;
        if (hurt > previousHurtTicks && c.player.method_24828() &&
                ThreadLocalRandom.current().nextInt(100) < chance.value) {
            jumpAt = System.currentTimeMillis() + reactionDelay.value;
        }
        previousHurtTicks = hurt;
        if (jumpAt >= 0 && System.currentTimeMillis() >= jumpAt) {
            jumpAt = -1;
            if (c.player.method_24828() && !c.player.method_5715() && c.client.field_1755 == null) {
                c.player.method_6043();
            }
        }
    }
}
