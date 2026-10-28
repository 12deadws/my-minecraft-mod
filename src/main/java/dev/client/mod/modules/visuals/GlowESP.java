package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import dev.client.mod.mixin.EntityAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Outline ESP: gives entities the glowing outline client-side, so they show through walls. */
public class GlowESP extends Module {
    private static final int GLOW_FLAG = 6;
    private static final String[] TARGETS = { "Players", "Mobs", "Items", "All" };
    private int target = 0;
    private final Set<Entity> glowing = Collections.newSetFromMap(new WeakHashMap<>());

    public GlowESP() {
        super("GlowESP", -1, false, "Visuals");
        settings.add(Settings.cycle("Targets", TARGETS, () -> target, v -> { clear(); target = v; }));
    }

    private boolean wants(Entity e) {
        boolean player = e instanceof Player && e != Util.mc.player;
        boolean mob = e instanceof Mob;
        boolean item = e instanceof ItemEntity;
        return switch (target) {
            case 0 -> player;
            case 1 -> mob;
            case 2 -> item;
            default -> player || mob || item;
        };
    }

    private static void setGlow(Entity e, boolean on) {
        ((EntityAccessor) e).caelestial$setSharedFlag(GLOW_FLAG, on);
    }

    private void clear() {
        for (Entity e : glowing) setGlow(e, false);
        glowing.clear();
    }

    @Override public void onTick() {
        ClientLevel level = Util.mc.level;
        if (level == null) return;
        for (Entity e : level.entitiesForRendering()) {
            if (wants(e)) {
                setGlow(e, true);      // re-applied every tick: the server's own flag updates overwrite it
                glowing.add(e);
            } else if (glowing.remove(e)) {
                setGlow(e, false);
            }
        }
    }

    @Override public void onDisable() { clear(); }
}
