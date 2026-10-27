package dev.client.mod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/** Settings file at .minecraft/config/caelestial.properties (edit by hand, or use the in-game hit chance keys). */
public class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("caelestial.properties");

    /** TriggerBot: percent chance (0-100) that it attacks on each attempt. */
    public static int triggerHitChance = 100;
    /** TriggerBot: random reaction delay in ticks (20 ticks = 1 second) between a target appearing and the attack. */
    public static int triggerMinDelay = 0;
    public static int triggerMaxDelay = 0;
    /** Reach: how far (in blocks) the client tries to hit while Reach is on. */
    public static double reachRange = 4.5;
    /** AutoPot: throws a heal potion at or below this many hearts. */
    public static double autoPotHearts = 3.0;

    /** Players (lowercase names) the macros/triggerbot will never auto-attack. */
    public static final Set<String> friends = new HashSet<>();

    public static boolean isFriend(String name) {
        return friends.contains(name.toLowerCase(Locale.ROOT));
    }

    public static void load() {
        if (!Files.exists(FILE)) { save(); return; }
        try (Reader r = Files.newBufferedReader(FILE)) {
            Properties p = new Properties();
            p.load(r);
            triggerHitChance = clamp(parseInt(p, "triggerbot.hitChance", 100));
            triggerMinDelay = Math.max(0, parseInt(p, "triggerbot.minDelayTicks", 0));
            triggerMaxDelay = Math.max(triggerMinDelay, parseInt(p, "triggerbot.maxDelayTicks", 0));
            reachRange = Math.max(3.0, Math.min(6.0, parseDouble(p, "reach.range", 4.5)));
            autoPotHearts = Math.max(0.5, Math.min(10.0, parseDouble(p, "autopot.hearts", 3.0)));
            friends.clear();
            for (String n : p.getProperty("friends", "").split(",")) {
                n = n.trim();
                if (!n.isEmpty()) friends.add(n.toLowerCase(Locale.ROOT));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("triggerbot.hitChance", Integer.toString(triggerHitChance));
        p.setProperty("triggerbot.minDelayTicks", Integer.toString(triggerMinDelay));
        p.setProperty("triggerbot.maxDelayTicks", Integer.toString(triggerMaxDelay));
        p.setProperty("reach.range", Double.toString(reachRange));
        p.setProperty("autopot.hearts", Double.toString(autoPotHearts));
        p.setProperty("friends", String.join(",", friends));
        try (Writer w = Files.newBufferedWriter(FILE)) {
            p.store(w, "Caelestial Client settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int parseInt(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private static double parseDouble(Properties p, String key, double def) {
        try { return Double.parseDouble(p.getProperty(key, Double.toString(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    public static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
