package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.player.LocalPlayer;

/** An expanding ring of particles at your feet each time you jump. */
public class JumpCircle extends Module {
    private int type = 0;
    private boolean wasGround = true;
    private int age = -1;                 // -1 = idle, otherwise ticks since the jump
    private double cx, cy, cz;

    public JumpCircle() {
        super("JumpCircle", -1, false, "Visuals");
        settings.add(Settings.cycle("Particle", Fx.NAMES, () -> type, v -> type = v));
    }

    @Override public void onTick() {
        LocalPlayer p = Util.mc.player;
        boolean ground = p.onGround();
        if (wasGround && !ground && p.getDeltaMovement().y > 0.1) {
            age = 0; cx = p.getX(); cy = p.getY() + 0.05; cz = p.getZ();
        }
        wasGround = ground;

        if (age >= 0) {
            double radius = 0.3 + age * 0.18;
            int n = 18 + age * 2;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2.0 * i / n;
                Fx.spawn(type, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, 0, 0, 0);
            }
            if (++age > 7) age = -1;
        }
    }

    @Override public void onDisable() { age = -1; wasGround = true; }
}
