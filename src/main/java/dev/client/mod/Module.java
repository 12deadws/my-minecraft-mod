package dev.client.mod;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public int key;                // GLFW key code, or -1 = no keybind (rebindable in the menu)
    public final boolean macro;    // macro = key press fires activate(); otherwise key toggles
    public final String category;  // menu section: "Combat", "Visuals", ...
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String name, int key, boolean macro) {
        this(name, key, macro, "Combat");
    }

    protected Module(String name, int key, boolean macro, String category) {
        this.name = name; this.key = key; this.macro = macro; this.category = category;
    }

    public void onTick() {}
    /** Called every frame while enabled, after the HUD is drawn. */
    public void onRender(GuiGraphics g) {}
    public void activate() {}
    public void onEnable() {}
    public void onDisable() {}

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }
}
