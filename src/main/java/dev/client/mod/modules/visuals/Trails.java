package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.player.LocalPlayer;

import java.util.concurrent.ThreadLocalRandom;

/** Leaves a particle trail at your feet while you move. */
public class Trails extends Module {
    private int type = 0;
    private int density = 2;
    private double lx, ly, lz;
    private boolean has = false;

    public Trails() {
        super("Trails", -1, false, "Visuals");
        settings.add(Settings.cycle("Particle", Fx.NAMES, () -> type, v -> type = v));
        settings.add(Settings.intRange("Density", 1, 6, 1, "", () -> density, v -> density = v));
    }

    @Override public void onTick() {
        LocalPlayer p = Util.mc.player;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        if (has) {
            double dx = x - lx, dz = z - lz;
            if (dx * dx + dz * dz > 0.0004 || Math.abs(y - ly) > 0.02) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < density; i++) {
                    Fx.spawn(type, x + r.nextDouble(-0.2, 0.2), y + 0.1 + r.nextDouble(0.0, 0.3),
                            z + r.nextDouble(-0.2, 0.2), 0, 0, 0);
                }
            }
        }
        lx = x; ly = y; lz = z; has = true;
    }

    @Override public void onDisable() { has = false; }
}
