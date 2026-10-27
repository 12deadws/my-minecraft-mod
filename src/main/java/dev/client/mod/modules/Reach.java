package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import org.lwjgl.glfw.GLFW;

/** Raises how far THIS client will try to hit (Config.reachRange). The server still has its own distance check. */
public class Reach extends Module {
    public Reach() {
        super("Reach", GLFW.GLFW_KEY_J, false);
        settings.add(new Setting("Range", () -> String.format(java.util.Locale.ROOT, "%.2f", Config.reachRange),
                () -> { Config.reachRange = Math.max(3.0, Config.reachRange - 0.25); Config.save(); },
                () -> { Config.reachRange = Math.min(6.0, Config.reachRange + 0.25); Config.save(); }));
    }
}
