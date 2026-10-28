package dev.cael3stis.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cael3stis.client.gui.Cael3stisScreen;
import dev.cael3stis.client.modules.AutoJumpReset;
import dev.cael3stis.client.modules.AutoTotem;
import dev.cael3stis.client.modules.AutoHealthPot;
import dev.cael3stis.client.modules.AutoRefill;
import dev.cael3stis.client.modules.SoftAim;
import dev.cael3stis.client.modules.TriggerBot;
import dev.cael3stis.client.modules.InfoHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1657;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client entrypoint. All client logic is dispatched on Minecraft's main thread. */
public final class Cael3stisClient implements ClientModInitializer {
    public static final String VERSION = "0.4.0";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("cael3stis.json");
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Set<String> FRIENDS = new HashSet<>();
    private static final Map<Integer, Boolean> PREVIOUS_KEYS = new HashMap<>();
    private static class_304 guiBind;

    @Override public void onInitializeClient() {
        // Register real implementations BEFORE reading their saved state.
        // Priority order: survival inventory > other inventory > movement > targeting > attacks.
        register(new AutoTotem());
        register(new AutoHealthPot());
        register(new AutoRefill());
        register(new AutoJumpReset());
        register(new SoftAim());
        register(new TriggerBot());
        register(new InfoHud());
        load();
        guiBind = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.cael3stis.open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT, class_304.class_11900.field_62556));
        ClientTickEvents.END_CLIENT_TICK.register(Cael3stisClient::tick);
        HudElementRegistry.addLast(class_2960.method_60655("cael3stis", "info_hud"), (drawContext, tickCounter) -> {
            for (Module module : MODULES) if (module instanceof InfoHud hud) hud.render(drawContext);
        });
        System.out.println("[Cael3stis] v" + VERSION + " loaded: " + MODULES.size() + " implemented modules.");
    }

    private static void tick(class_310 client) {
        while (guiBind.method_1436()) {
            if (client.field_1755 instanceof Cael3stisScreen) client.method_1507(null);
            else if (client.field_1755 == null) client.method_1507(new Cael3stisScreen());
        }
        if (client.field_1724 == null || client.field_1687 == null) {
            for (Module module : MODULES) module.reset();
            PREVIOUS_KEYS.clear();
            return;
        }
        if (client.field_1755 != null) {
            PREVIOUS_KEYS.clear();
            return;
        }
        long window = client.method_22683().method_4490();
        for (Module module : MODULES) {
            int key = module.key;
            if (key == GLFW.GLFW_KEY_UNKNOWN || key == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
            boolean down = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            if (down && !PREVIOUS_KEYS.getOrDefault(key, false)) module.toggle();
            PREVIOUS_KEYS.put(key, down);
        }
        ActionCoordinator.startTick();
        GameContext context = new GameContext(client);
        for (Module module : MODULES) {
            if (!module.enabled) continue;
            try { module.tick(context); }
            catch (Exception e) { System.err.println("[Cael3stis] " + module.name + ": " + e); module.setEnabled(false); }
        }
    }

    public static List<Module> modules() { return Collections.unmodifiableList(MODULES); }

    private static void register(Module module) {
        if (MODULES.stream().anyMatch(x -> x.name.equals(module.name))) throw new IllegalStateException("Duplicate " + module.name);
        MODULES.add(module);
    }

    public static boolean isEnemy(class_1657 local, class_1657 other) {
        return other != null && other != local && other.method_5805() && !other.method_7325()
                && !local.method_5722(other) && !FRIENDS.contains(other.method_5477().getString().toLowerCase(java.util.Locale.ROOT));
    }

    public static Set<String> friends() { return Collections.unmodifiableSet(FRIENDS); }
    public static void addFriend(String name) {
        String normal = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normal.isBlank() && normal.length() <= 16) { FRIENDS.add(normal); save(); }
    }
    public static void removeFriend(String name) {
        FRIENDS.remove(name.trim().toLowerCase(java.util.Locale.ROOT)); save();
    }
    /** Reassigns a key and clears any conflicting module assignment. */
    public static void assignKey(Module module, int key) {
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) key = GLFW.GLFW_KEY_UNKNOWN;
        for (Module other : MODULES) if (other != module && other.key == key && key != GLFW.GLFW_KEY_UNKNOWN) other.key = GLFW.GLFW_KEY_UNKNOWN;
        module.key = key;
        PREVIOUS_KEYS.clear();
        save();
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("configVersion", VERSION);
        JsonObject mods = new JsonObject();
        for (Module module : MODULES) mods.add(module.name, module.json());
        root.add("modules", mods);
        com.google.gson.JsonArray friends = new com.google.gson.JsonArray();
        FRIENDS.stream().sorted().forEach(friends::add);
        root.add("friends", friends);
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) { GSON.toJson(root, writer); }
        } catch (Exception ex) { System.err.println("[Cael3stis] Config save failed: " + ex); }
    }

    private static void load() {
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            JsonElement tree = JsonParser.parseReader(reader);
            if (!tree.isJsonObject()) return;
            JsonObject root = tree.getAsJsonObject();
            // Accept legacy v0.3 root structure as well as the new versioned structure.
            JsonObject mods = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : root;
            for (Module module : MODULES) if (mods.has(module.name) && mods.get(module.name).isJsonObject()) module.read(mods.getAsJsonObject(module.name));
            if (root.has("friends") && root.get("friends").isJsonArray()) {
                for (JsonElement friend : root.getAsJsonArray("friends")) if (friend.isJsonPrimitive()) FRIENDS.add(friend.getAsString().toLowerCase(java.util.Locale.ROOT));
            }
            // Resolve inconsistent keybinds in saved JSON; Right Shift belongs to GUI only.
            Set<Integer> seen = new HashSet<>();
            for (Module module : MODULES) if (module.key != GLFW.GLFW_KEY_UNKNOWN && !seen.add(module.key)) module.key = GLFW.GLFW_KEY_UNKNOWN;
        } catch (Exception ex) { System.err.println("[Cael3stis] Config load failed: " + ex); }
    }
}
