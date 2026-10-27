package dev.client.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import dev.client.mod.gui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class MyClient implements ClientModInitializer {
    private final Map<Integer, Boolean> down = new HashMap<>();

    @Override
    public void onInitializeClient() {
        Config.load();
        ModuleManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // Keybinds (polled, so no KeyMapping/category registration needed).
        // If your mappings name the window handle differently, change getWindow().handle().
        if (mc.screen == null) {
            long window = mc.getWindow().handle();

            // Right Shift opens the Caelestial Client menu
            boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            boolean shiftWas = down.getOrDefault(GLFW.GLFW_KEY_RIGHT_SHIFT, false);
            down.put(GLFW.GLFW_KEY_RIGHT_SHIFT, shift);
            if (shift && !shiftWas) {
                mc.setScreen(new ClickGuiScreen());
                return;
            }
            for (Module m : ModuleManager.modules) {
                boolean pressed = GLFW.glfwGetKey(window, m.key) == GLFW.GLFW_PRESS;
                boolean was = down.getOrDefault(m.key, false);
                if (pressed && !was) {
                    if (m.macro) m.activate();
                    else {
                        m.toggle();
                        mc.player.displayClientMessage(Component.literal(m.name + (m.enabled ? " §aON" : " §cOFF")), true);
                    }
                }
                down.put(m.key, pressed);
            }

            // Hit chance: [ lowers, ] raises (steps of 5)
            adjustChance(mc, window, GLFW.GLFW_KEY_LEFT_BRACKET, -5);
            adjustChance(mc, window, GLFW.GLFW_KEY_RIGHT_BRACKET, 5);
        }

        for (Module m : ModuleManager.modules) {
            if (m.macro || m.enabled) m.onTick();
        }
    }

    private void adjustChance(Minecraft mc, long window, int key, int delta) {
        boolean pressed = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
        if (pressed && !down.getOrDefault(key, false)) {
            Config.triggerHitChance = Config.clamp(Config.triggerHitChance + delta);
            Config.save();
            mc.player.displayClientMessage(Component.literal("TriggerBot hit chance: " + Config.triggerHitChance + "%"), true);
        }
        down.put(key, pressed);
    }
}
