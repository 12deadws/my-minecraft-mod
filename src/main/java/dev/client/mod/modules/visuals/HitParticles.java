package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

/** A burst of particles on whatever you hit. Called from MultiPlayerGameModeMixin. */
public class HitParticles extends Module {
    private int type = 6;      // Crit
    private int amount = 12;

    public HitParticles() {
        super("HitParticles", -1, false, "Visuals");
        settings.add(Settings.cycle("Particle", Fx.NAMES, () -> type, v -> type = v));
        settings.add(Settings.intRange("Amount", 4, 40, 4, "", () -> amount, v -> amount = v));
    }

    public void onAttack(Entity t) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double w = t.getBbWidth(), h = t.getBbHeight();
        for (int i = 0; i < amount; i++) {
            Fx.spawn(type,
                    t.getX() + r.nextDouble(-w / 2, w / 2),
                    t.getY() + r.nextDouble(0.0, h),
                    t.getZ() + r.nextDouble(-w / 2, w / 2),
                    r.nextDouble(-0.15, 0.15), r.nextDouble(0.0, 0.2), r.nextDouble(-0.15, 0.15));
        }
    }
}
