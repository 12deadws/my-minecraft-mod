package dev.client.mod;

import dev.client.mod.gui.ClickGuiScreen;
import dev.client.mod.modules.AutoCrit;
import dev.client.mod.modules.Esp;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class MyClient implements ClientModInitializer {
    private final Map<Integer, Boolean> down = new HashMap<>();

    @Override
    public void onInitializeClient() {
        Config.load();
        ModuleManager.init();
        Config.applyKeys();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        // AutoCrit: runs before ANY attack you make (manual clicks and macros alike)
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            AutoCrit crit = ModuleManager.get(AutoCrit.class);
            if (crit != null && crit.enabled && player instanceof LocalPlayer lp && entity instanceof LivingEntity) {
                crit.sendCritPackets(lp);
            }
            return InteractionResult.PASS;
        });

        // ESP is drawn on the HUD layer
        HudRenderCallback.EVENT.register((graphics, delta) -> {
            Esp esp = ModuleManager.get(Esp.class);
            if (esp != null && esp.enabled) esp.render(graphics, delta.getGameTimeDeltaPartialTick(true));
        });
    }

    private void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

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

            // Module keybinds (-1 = NONE)
            for (Module m : ModuleManager.modules) {
                if (m.key < 0 || m.key == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
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
        }

        for (Module m : ModuleManager.modules) {
            if (m.macro || m.enabled) m.onTick();
        }
    }
}
