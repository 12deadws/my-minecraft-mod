package dev.cael3stis.client;

import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;

/** Minecraft objects available to a module during a client tick. */
public final class GameContext {
    public final class_310 client;
    public final class_746 player;
    public final class_638 world;

    public GameContext(class_310 client) {
        this.client = client;
        this.player = client.field_1724;
        this.world = client.field_1687;
    }
}
