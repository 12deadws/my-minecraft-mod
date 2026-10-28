package com.caelestis.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.caelestis.client.CaelestisClient;
import com.caelestis.client.module.Module;
import com.caelestis.client.module.ModuleManager;
import com.caelestis.client.setting.Setting;
import com.caelestis.client.util.Friends;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON persistence: settings, keybinds, friends and GUI window positions.
 * Module enabled-state is intentionally NOT saved: every module starts disabled each launch.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("caelestis_client.json");

    /** Panel positions: category name -> {x, y}. */
    public static final Map<String, float[]> PANEL_POS = new HashMap<>();

    private static ModuleManager modules;

    private ConfigManager() {
    }

    public static synchronized void init(ModuleManager manager) {
        modules = manager;
        reload();
    }

    public static synchronized void reload() {
        if (modules == null || !Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;

            JsonObject mods = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : modules.all()) {
                if (!mods.has(m.name) || !mods.get(m.name).isJsonObject()) continue;
                JsonObject mo = mods.getAsJsonObject(m.name);
                if (m.toggleable && mo.has("keybind") && mo.get("keybind").isJsonPrimitive()) {
                    int key = mo.get("keybind").getAsInt();
                    // Never allow the reserved GUI key, and resolve duplicates from hand-edited files.
                    if (key != ModuleManager.GUI_KEY) modules.assignKey(m, key);
                }
                if (mo.has("settings") && mo.get("settings").isJsonObject()) {
                    JsonObject so = mo.getAsJsonObject("settings");
                    for (Setting s : m.settings) {
                        if (so.has(s.name)) s.load(so.get(s.name));
                    }
                }
            }

            Friends.clear();
            if (root.has("friends") && root.get("friends").isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray("friends")) Friends.add(e.getAsString());
            }

            PANEL_POS.clear();
            if (root.has("gui") && root.get("gui").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("gui").entrySet()) {
                    if (e.getValue().isJsonArray() && e.getValue().getAsJsonArray().size() == 2) {
                        JsonArray a = e.getValue().getAsJsonArray();
                        PANEL_POS.put(e.getKey(), new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()});
                    }
                }
            }
        } catch (Exception ex) {
            CaelestisClient.LOG.error("Could not read config, using defaults", ex);
        }
    }

    public static synchronized void save() {
        if (modules == null) return;
        JsonObject root = new JsonObject();
        root.addProperty("version", CaelestisClient.VERSION);

        JsonObject mods = new JsonObject();
        for (Module m : modules.all()) {
            JsonObject mo = new JsonObject();
            if (m.toggleable) mo.addProperty("keybind", m.keybind);
            JsonObject so = new JsonObject();
            for (Setting s : m.settings) {
                JsonElement v = s.save();
                if (v != null) so.add(s.name, v);
            }
            mo.add("settings", so);
            mods.add(m.name, mo);
        }
        root.add("modules", mods);

        JsonArray friends = new JsonArray();
        for (String f : Friends.snapshot()) friends.add(f);
        root.add("friends", friends);

        JsonObject gui = new JsonObject();
        for (Map.Entry<String, float[]> e : PANEL_POS.entrySet()) {
            JsonArray a = new JsonArray();
            a.add(e.getValue()[0]);
            a.add(e.getValue()[1]);
            gui.add(e.getKey(), a);
        }
        root.add("gui", gui);

        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            CaelestisClient.LOG.error("Could not save config", ex);
        }
    }
}
