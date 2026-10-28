package com.caelestis.client.module;

import com.caelestis.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all modules. Modules are disabled by default and have no default keybind.
 * onTick() is only called while the module is enabled, a world is loaded and no screen is open.
 */
public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;
    /** Lower runs earlier each tick (survival modules first). */
    public final int priority;
    /** False for configuration-only entries in the Settings tab. */
    public final boolean toggleable;

    public final List<Setting> settings = new ArrayList<>();
    /** GLFW key code, -1 = NONE. */
    public int keybind = -1;

    private volatile boolean enabled = false;

    protected Module(String name, String description, Category category, int priority) {
        this(name, description, category, priority, true);
    }

    protected Module(String name, String description, Category category, int priority, boolean toggleable) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.toggleable = toggleable;
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (!toggleable || value == enabled) return;
        enabled = value;
        try {
            if (value) onEnable();
            else onDisable();
        } catch (Throwable t) {
            com.caelestis.client.CaelestisClient.LOG.error("Module {} failed in enable/disable", name, t);
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    /** Called every client tick while enabled and in a world with no screen open. */
    public void onTick() {
    }

    /** Called when the world changes or the player disconnects. Reset all transient state here. */
    public void onWorldChange() {
    }

    /** Per-frame 2D overlay (HUD layer). Called while enabled and in a world. */
    public void onRender(DrawContext ctx, float tickDelta) {
    }

    /** Per-frame hook (used for smooth camera work). */
    public void onFrame(float deltaSeconds) {
    }

    /** Short status text for the HUD module list, or null. */
    public String hudInfo() {
        return null;
    }
}
