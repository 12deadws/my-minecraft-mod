package com.caelestis.client.module;

import com.caelestis.client.CaelestisClient;
import com.caelestis.client.gui.ClickGuiScreen;
import com.caelestis.client.modules.*;
import com.caelestis.client.util.Targets;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_638;

public class ModuleManager {
    /** Reserved for the ClickGUI. Cannot be bound to a module. */
    public static final int GUI_KEY = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private final List<Module> all = new ArrayList<>();
    private final List<Module> tickOrder = new ArrayList<>();
    private final Map<Module, Boolean> keyWasDown = new HashMap<>();
    private final Map<Module, Long> failed = new HashMap<>();

    private boolean guiKeyWasDown = false;
    private boolean wasInWorld = false;
    private class_638 lastWorld = null;
    private long lastFrameNanos = 0;

    public final ClientSettings clientSettings = new ClientSettings();

    public ModuleManager() {
        // Combat
        all.add(new TriggerBot());
        all.add(new AutoStunSlam());
        all.add(new AutoHealthPot());
        all.add(new AutoJumpReset());
        all.add(new SoftAim());
        // Player
        all.add(new AutoTotem());
        all.add(new AutoRefill());
        // Crystal PvP
        all.add(new CrystalAura());
        all.add(new Surround());
        all.add(new AutoTrap());
        all.add(new AnchorAura());
        all.add(new AntiCrystal());
        // Render
        all.add(new HoleESP());
        all.add(new NameTags());
        // Settings tab
        all.add(clientSettings);
        all.add(new FriendsSettings());

        tickOrder.addAll(all);
        tickOrder.sort(Comparator.comparingInt(m -> m.priority));
    }

    public List<Module> all() {
        return all;
    }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : all) if (m.category == c) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module m : all) if (type.isInstance(m)) return (T) m;
        return null;
    }

    // ------------------------------------------------------------------ keybinds

    /**
     * Assigns a key, resolving conflicts. Returns a message for the user, or null.
     * Right Shift is reserved for the ClickGUI and is refused.
     */
    public String assignKey(Module m, int key) {
        if (key == GUI_KEY) return "Right Shift is reserved for the ClickGUI.";
        String msg = null;
        if (key >= 0) {
            for (Module other : all) {
                if (other != m && other.keybind == key) {
                    other.keybind = -1;
                    msg = "Unbound " + other.name + " (key conflict)";
                }
            }
        }
        m.keybind = key;
        return msg;
    }

    private void pollKeys(class_310 mc) {
        long window = mc.method_22683().method_4490();
        boolean screenOpen = mc.field_1755 != null;

        // GUI key: only opens when no other screen is up (closing is handled inside the GUI itself).
        boolean guiDown = GLFW.glfwGetKey(window, GUI_KEY) == GLFW.GLFW_PRESS;
        if (guiDown && !guiKeyWasDown && !screenOpen) {
            mc.method_1507(new ClickGuiScreen(this));
        }
        guiKeyWasDown = guiDown;

        for (Module m : all) {
            if (!m.toggleable || m.keybind < 0) continue;
            boolean down = GLFW.glfwGetKey(window, m.keybind) == GLFW.GLFW_PRESS;
            boolean was = keyWasDown.getOrDefault(m, false);
            if (down && !was && !screenOpen) m.toggle();
            keyWasDown.put(m, down);
        }
    }

    // ------------------------------------------------------------------ lifecycle

    public void tick(class_310 mc) {
        if (mc.field_1724 == null || mc.field_1687 == null) {
            if (wasInWorld) {
                cleanup();
                wasInWorld = false;
                lastWorld = null;
            }
            return;
        }
        if (!wasInWorld || mc.field_1687 != lastWorld) {
            cleanup(); // joined, respawned across dimensions, or changed world
            lastWorld = mc.field_1687;
            wasInWorld = true;
        }

        ActionManager.beginTick();
        pollKeys(mc);

        if (mc.field_1755 != null || mc.field_1724.method_29504()) return;

        for (Module m : tickOrder) {
            if (!m.isEnabled()) continue;
            try {
                m.onTick();
            } catch (Throwable t) {
                // A bug in one module must never crash the game: log it and switch the module off.
                CaelestisClient.LOG.error("Module {} crashed and was disabled", m.name, t);
                m.setEnabled(false);
            }
        }
    }

    public void render(class_332 ctx, float tickDelta) {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null) {
            lastFrameNanos = 0;
            return;
        }
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;

        for (Module m : tickOrder) {
            if (!m.isEnabled()) continue;
            try {
                m.onFrame(dt);
                m.onRender(ctx, tickDelta);
            } catch (Throwable t) {
                CaelestisClient.LOG.error("Module {} crashed while rendering and was disabled", m.name, t);
                m.setEnabled(false);
            }
        }
    }

    /** World change / disconnect: reset every module's transient state and the shared systems. */
    public void cleanup() {
        for (Module m : all) {
            try {
                m.onWorldChange();
            } catch (Throwable t) {
                CaelestisClient.LOG.error("Module {} failed during cleanup", m.name, t);
            }
        }
        ActionManager.reset();
        Targets.reset();
        keyWasDown.clear();
    }
}
