package dev.client.mod.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets GlowESP flip an entity's client-side shared flags (flag 6 = glowing outline). */
@Mixin(Entity.class)
public interface EntityAccessor {
    @Invoker("setSharedFlag")
    void caelestial$setSharedFlag(int flag, boolean value);
}
