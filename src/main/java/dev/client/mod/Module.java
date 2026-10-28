package dev.client.mod;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public int key;                // GLFW key code, -1 = NONE
    public final boolean macro;    // macro = key press fires activate(); otherwise key toggles
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String name, int key, boolean macro) {
        this.name = name; this.key = key; this.macro = macro;
    }

    public void onTick() {}
    public void activate() {}
    public void onEnable() {}
    public void onDisable() {}

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }
}
