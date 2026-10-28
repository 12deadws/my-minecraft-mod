package dev.client.mod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Settings file at .minecraft/config/caelestial.properties (the menu edits it for you). */
public class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("caelestial.properties");

    public static int triggerHitChance = 100;
    public static int triggerMinDelay = 0;
    public static int triggerMaxDelay = 0;
    public static double reachRange = 4.5;
    public static double autoPotHearts = 3.0;

    public static boolean espChests = true;
    public static boolean espShulkers = true;
    public static boolean espSpawners = true;
    public static int espRange = 64;

    /** Players (lowercase names) the macros/triggerbot will never auto-attack. */
    public static final Set<String> friends = new HashSet<>();

    private static final Map<String, Integer> savedKeys = new HashMap<>();

    public static boolean isFriend(String name) {
        return friends.contains(name.toLowerCase(Locale.ROOT));
    }

    /** Every module starts with no keybind (-1) unless one was saved. */
    public static void applyKeys() {
        for (Module m : ModuleManager.modules) m.key = savedKeys.getOrDefault(m.name, -1);
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
            espChests = Boolean.parseBoolean(p.getProperty("esp.chests", "true"));
            espShulkers = Boolean.parseBoolean(p.getProperty("esp.shulkers", "true"));
            espSpawners = Boolean.parseBoolean(p.getProperty("esp.spawners", "true"));
            espRange = Math.max(16, Math.min(128, parseInt(p, "esp.range", 64)));
            friends.clear();
            for (String n : p.getProperty("friends", "").split(",")) {
                n = n.trim();
                if (!n.isEmpty()) friends.add(n.toLowerCase(Locale.ROOT));
            }
            savedKeys.clear();
            for (String name : p.stringPropertyNames()) {
                if (name.startsWith("key.")) {
                    try { savedKeys.put(name.substring(4), Integer.parseInt(p.getProperty(name).trim())); }
                    catch (NumberFormatException ignored) {}
                }
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
        p.setProperty("esp.chests", Boolean.toString(espChests));
        p.setProperty("esp.shulkers", Boolean.toString(espShulkers));
        p.setProperty("esp.spawners", Boolean.toString(espSpawners));
        p.setProperty("esp.range", Integer.toString(espRange));
        p.setProperty("friends", String.join(",", friends));
        for (Map.Entry<String, Integer> e : savedKeys.entrySet()) p.setProperty("key." + e.getKey(), Integer.toString(e.getValue()));
        for (Module m : ModuleManager.modules) {
            savedKeys.put(m.name, m.key);
            p.setProperty("key." + m.name, Integer.toString(m.key));
        }
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
