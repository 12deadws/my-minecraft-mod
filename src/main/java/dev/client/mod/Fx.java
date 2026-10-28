package dev.client.mod;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** Particle choices shared by Trails, JumpCircle and HitParticles. Particles are client-side only. */
public final class Fx {
    public static final String[] NAMES = { "End rod", "Flame", "Soul flame", "Heart", "Villager", "Cloud", "Crit" };
    public static final ParticleOptions[] TYPES = {
            ParticleTypes.END_ROD, ParticleTypes.FLAME, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.HEART,
            ParticleTypes.HAPPY_VILLAGER, ParticleTypes.CLOUD, ParticleTypes.CRIT
    };

    private Fx() {}

    public static void spawn(int type, double x, double y, double z, double vx, double vy, double vz) {
        ClientLevel level = Util.mc.level;
        if (level != null) level.addParticle(TYPES[type], x, y, z, vx, vy, vz);
    }
}
