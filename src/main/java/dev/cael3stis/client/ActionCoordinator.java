package dev.cael3stis.client;

/**
 * One inventory or active item/attack operation per tick.
 * Modules are dispatched in priority order by Cael3stisClient.
 * This is client-side coordination, not an anti-cheat bypass.
 */
public final class ActionCoordinator {
    private static String owner;
    private ActionCoordinator() {}

    public static void startTick() { owner = null; }

    public static boolean claim(String moduleName) {
        if (owner != null) return false;
        owner = moduleName;
        return true;
    }

    public static boolean isBusy() { return owner != null; }
}
