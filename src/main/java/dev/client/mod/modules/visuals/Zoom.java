package dev.client.mod.modules.visuals;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.OptionInstance;

/** Toggle zoom by lowering the FOV setting (restored when you turn it off). Vanilla's minimum FOV is 30. */
public class Zoom extends Module {
    private int zoomFov = 30;
    private int saved = 70;

    public Zoom() {
        super("Zoom", -1, false, "Visuals");
        settings.add(Settings.intRange("FOV", 30, 70, 5, "", () -> zoomFov, v -> zoomFov = v));
    }

    private void apply() {
        OptionInstance<Integer> fov = Util.mc.options.fov();
        if (fov.get() != zoomFov) fov.set(zoomFov);
    }

    @Override public void onEnable() { saved = Util.mc.options.fov().get(); apply(); }
    @Override public void onTick() { apply(); }
    @Override public void onDisable() { Util.mc.options.fov().set(saved); }
}
