package dev.client.mod;

import dev.client.mod.modules.*;
import dev.client.mod.modules.visuals.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.add(new TriggerBot());   // R
        modules.add(new StunSlam());     // G (macro)
        modules.add(new AutoCrit());     // C
        modules.add(new LungeMacro());   // H
        modules.add(new Reach());        // J
        modules.add(new Fly());          // (no default key)
        modules.add(new AutoPot());      // P

        // Visuals (no default keybinds: toggle them from the menu)
        modules.add(new GlowESP());
        modules.add(new NightVision());
        modules.add(new Zoom());
        modules.add(new Trails());
        modules.add(new JumpCircle());
        modules.add(new HitParticles());
        modules.add(new InfoHud());
        modules.add(new KeystrokesHud());
        modules.add(new PotionsHud());
        modules.add(new TargetHud());
        modules.add(new ArrayListHud());

        Binds.applySaved();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> c) {
        for (Module m : modules) if (c.isInstance(m)) return (T) m;
        return null;
    }
}
