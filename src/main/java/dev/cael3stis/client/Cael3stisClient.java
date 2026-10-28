package dev.cael3stis.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.item.Items;
import java.util.concurrent.ThreadLocalRandom;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Cael3stisClient implements ClientModInitializer {
    public static final String NAME = "Cael3stis Client";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Map<String, Module> MODULES = new LinkedHashMap<>();
    private static final Path CONFIG = Path.of("config", "cael3stis.json");
    private static KeyBinding guiKey;
    private static boolean lastGuiPressed;
    private static int triggerDelayMs = 100;
    private static int jumpChance = 100;
    private static int jumpDelayMs = 50;
    private static long nextAttackAt;
    private static long jumpAt = -1;
    private static int lastHurtTime;
    private static final Map<String, Boolean> lastKeys = new LinkedHashMap<>();

    @Override public void onInitializeClient() {
        add("TriggerBot", "Combat"); add("Auto Stun Slam", "Combat");
        add("Auto Health Pot", "Combat"); add("Auto Jump Reset", "Combat");
        add("Soft Aim", "Combat"); add("Auto Totem", "Player");
        add("Auto Refill", "Player"); add("Crystal Aura", "Cpvp");
        add("Surround", "Cpvp"); add("Auto Trap", "Cpvp");
        add("Anchor Aura", "Cpvp"); add("Hole ESP", "Cpvp");
        add("Anti Crystal", "Cpvp"); add("NameTags", "Render");
        add("Player ESP", "Render");
        load();
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cael3stis.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean pressed = guiKey.isPressed();
            if (pressed && !lastGuiPressed) {
                if (client.currentScreen instanceof ClientScreen) client.setScreen(null);
                else client.setScreen(new ClientScreen());
            }
            lastGuiPressed = pressed;
            if (client.player == null || client.world == null) {
                jumpAt = -1;
                lastHurtTime = 0;
                return;
            }
            tickGameplay(client);
            long window = client.getWindow().getHandle();
            for (Module m : MODULES.values()) {
                boolean down = m.key != GLFW.GLFW_KEY_UNKNOWN && m.key != GLFW.GLFW_KEY_RIGHT_SHIFT
                        && GLFW.glfwGetKey(window, m.key) == GLFW.GLFW_PRESS;
                boolean wasDown = lastKeys.getOrDefault(m.name, false);
                if (down && !wasDown && client.currentScreen == null) m.toggle(client);
                lastKeys.put(m.name, down);
            }
        });
    }
    private static void tickGameplay(MinecraftClient client) {
        // Gameplay actions run only on the client thread, and never while a screen is open.
        if (client.currentScreen != null || client.interactionManager == null) return;
        long now = System.currentTimeMillis();
        if (MODULES.get("TriggerBot").enabled && now >= nextAttackAt
                && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity target
                && target.isAlive() && target != client.player
                && !target.isTeammate(client.player)
                && client.player.canSee(target)
                && client.player.distanceTo(target) <= 3.0f
                && (client.player.getMainHandStack().getItem() instanceof SwordItem
                    || client.player.getMainHandStack().getItem() instanceof AxeItem)
                && client.player.getAttackCooldownProgress(0) >= 0.99f) {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            nextAttackAt = now + triggerDelayMs;
        }
        // A conservative jump-reset attempt on a new hurt animation; not a guarantee
        // that the server's knockback timing will allow a reset.
        int hurt = client.player.hurtTime;
        if (MODULES.get("Auto Jump Reset").enabled && hurt > lastHurtTime
                && client.player.isOnGround()
                && ThreadLocalRandom.current().nextInt(100) < jumpChance) {
            jumpAt = now + jumpDelayMs;
        }
        if (jumpAt >= 0 && now >= jumpAt) {
            if (client.player.isOnGround()) client.player.jump();
            jumpAt = -1;
        }
        lastHurtTime = hurt;
    }
    public static int getSetting(String name) {
        return switch (name) {
            case "Trigger delay (ms)" -> triggerDelayMs;
            case "Jump chance (%)" -> jumpChance;
            case "Jump delay (ms)" -> jumpDelayMs;
            default -> 0;
        };
    }
    public static void adjustSetting(String name, int amount) {
        switch (name) {
            case "Trigger delay (ms)" -> triggerDelayMs = Math.clamp(triggerDelayMs + amount * 10, 0, 500);
            case "Jump chance (%)" -> jumpChance = Math.clamp(jumpChance + amount * 5, 0, 100);
            case "Jump delay (ms)" -> jumpDelayMs = Math.clamp(jumpDelayMs + amount * 10, 0, 200);
        }
        save();
    }
    private static void add(String name, String category) { MODULES.put(name, new Module(name, category)); }
    public static void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            var root = new com.google.gson.JsonObject();
            root.add("modules", GSON.toJsonTree(MODULES));
            var settings = new com.google.gson.JsonObject();
            settings.addProperty("triggerDelayMs", triggerDelayMs);
            settings.addProperty("jumpChance", jumpChance);
            settings.addProperty("jumpDelayMs", jumpDelayMs);
            root.add("settings", settings);
            Files.writeString(CONFIG, GSON.toJson(root));
        } catch (IOException ex) { System.err.println("[Cael3stis] Could not save configuration: " + ex); }
    }
    private static void load() {
        if (!Files.exists(CONFIG)) return;
        try {
            var root = com.google.gson.JsonParser.parseString(Files.readString(CONFIG)).getAsJsonObject();
            if (root.has("settings")) {
                var settings = root.getAsJsonObject("settings");
                if (settings.has("triggerDelayMs")) triggerDelayMs = Math.clamp(settings.get("triggerDelayMs").getAsInt(), 0, 500);
                if (settings.has("jumpChance")) jumpChance = Math.clamp(settings.get("jumpChance").getAsInt(), 0, 100);
                if (settings.has("jumpDelayMs")) jumpDelayMs = Math.clamp(settings.get("jumpDelayMs").getAsInt(), 0, 200);
            }
            var modules = root.has("modules") ? root.getAsJsonObject("modules") : root;
            for (var entry : MODULES.entrySet()) {
                if (!modules.has(entry.getKey())) continue;
                var json = modules.getAsJsonObject(entry.getKey());
                if (json.has("enabled")) entry.getValue().enabled = json.get("enabled").getAsBoolean();
                if (json.has("key")) entry.getValue().key = json.get("key").getAsInt();
            }
        } catch (Exception ex) { System.err.println("[Cael3stis] Could not read configuration: " + ex); }
    }
    public static final class Module {
        public final String name, category;
        public boolean enabled = false;
        public int key = GLFW.GLFW_KEY_UNKNOWN;
        private Module(String name, String category) { this.name = name; this.category = category; }
        public void toggle(MinecraftClient client) {
            enabled = !enabled; save();
            if (client.player != null) client.player.sendMessage(Text.literal(name + ": " + (enabled ? "ON" : "OFF") + (name.equals("TriggerBot") || name.equals("Auto Jump Reset") ? "" : " (not implemented)")), true);
        }
    }
}
