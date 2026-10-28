package dev.client.mod.mixin;

import dev.client.mod.ModuleManager;
import dev.client.mod.modules.AutoCrit;
import dev.client.mod.modules.visuals.HitParticles;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wraps every client attack so AutoCrit applies to your own clicks too, not just the macro modules. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void caelestial$beforeAttack(Player player, Entity target, CallbackInfo ci) {
        AutoCrit crit = ModuleManager.get(AutoCrit.class);
        if (crit != null && crit.enabled && player instanceof LocalPlayer lp) crit.beforeAttack(lp);
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void caelestial$afterAttack(Player player, Entity target, CallbackInfo ci) {
        AutoCrit crit = ModuleManager.get(AutoCrit.class);
        if (crit != null && crit.enabled && player instanceof LocalPlayer lp) crit.afterAttack(lp);

        HitParticles hit = ModuleManager.get(HitParticles.class);
        if (hit != null && hit.enabled && player instanceof LocalPlayer) hit.onAttack(target);
    }
}
