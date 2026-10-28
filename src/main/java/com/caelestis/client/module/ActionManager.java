package com.caelestis.client.module;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shared action-priority system.
 *
 * Modules tick in priority order (survival first). Before performing an action that touches a shared
 * resource (attacking, changing the held hotbar slot, moving inventory items, rotating the camera) a
 * module must claim the matching channel. A claim fails if a module that ticked earlier already used the
 * channel this tick, or if another module is holding it across several ticks (multi-step sequences such as
 * the axe-to-mace stun slam).
 */
public final class ActionManager {
    public enum Channel {ATTACK, HOTBAR, INVENTORY, ROTATION}

    private static final Map<Channel, Object> owner = new EnumMap<>(Channel.class);
    private static final Map<Channel, Integer> heldUntil = new EnumMap<>(Channel.class);
    private static final Map<Channel, Integer> lastUsed = new EnumMap<>(Channel.class);
    private static int tick = 0;

    private ActionManager() {
    }

    public static synchronized void beginTick() {
        tick++;
        for (Channel c : Channel.values()) {
            Integer until = heldUntil.get(c);
            if (until == null || until < tick) {
                owner.remove(c);
            }
        }
    }

    /** Try to use a channel this tick. Returns true if the caller may act. */
    public static synchronized boolean claim(Channel c, Object who) {
        Object o = owner.get(c);
        if (o != null && o != who) return false;
        owner.put(c, who);
        heldUntil.merge(c, tick, Math::max);
        lastUsed.put(c, tick);
        return true;
    }

    /** Claim and keep the channel for the next {@code ticks} ticks (multi-tick sequences). */
    public static synchronized boolean hold(Channel c, Object who, int ticks) {
        if (!claim(c, who)) return false;
        heldUntil.put(c, tick + Math.max(0, ticks));
        return true;
    }

    public static synchronized void release(Channel c, Object who) {
        if (owner.get(c) == who) {
            owner.remove(c);
            heldUntil.put(c, tick - 1);
        }
    }

    public static synchronized boolean isFree(Channel c, Object who) {
        Object o = owner.get(c);
        return o == null || o == who;
    }

    public static synchronized int ticksSinceUse(Channel c) {
        Integer t = lastUsed.get(c);
        return t == null ? Integer.MAX_VALUE : tick - t;
    }

    public static synchronized void reset() {
        owner.clear();
        heldUntil.clear();
        lastUsed.clear();
    }
}
