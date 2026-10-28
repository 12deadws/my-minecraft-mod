package dev.client.mod;

import dev.client.mod.modules.*;
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
        modules.add(new Fly());          // F
        modules.add(new AutoPot());      // P
        modules.add(new Esp());          // storage / spawner ESP
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> c) {
        for (Module m : modules) if (c.isInstance(m)) return (T) m;
        return null;
    }
}
