package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Full brightness via a client-side night vision effect (no particles). */
public class NightVision extends Module {
    private boolean applied = false;

    public NightVision() { super("NightVision", -1, false, "Visuals"); }

    @Override public void onTick() {
        LocalPlayer p = Util.mc.player;
        MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
        // Re-apply when missing, or before our own copy runs low (night vision flickers under 10s).
        if (cur == null || (applied && cur.getDuration() < 400)) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false));
            applied = true;
        }
    }

    @Override public void onDisable() {
        LocalPlayer p = Util.mc.player;
        if (applied && p != null) p.removeEffect(MobEffects.NIGHT_VISION);
        applied = false;
    }
}
