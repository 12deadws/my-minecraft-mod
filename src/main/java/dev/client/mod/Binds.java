package dev.client.mod;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Everything that can be bound to a key (every module plus the client-wide keys), and the rules for rebinding. */
public final class Binds {
    public record Target(String id, String label, IntSupplier get, IntConsumer set, boolean clearable) {}
    public record Result(boolean ok, String msg) {}

    public static final Target MENU = new Target("client:menu", "Menu key",
            () -> Config.menuKey, k -> Config.menuKey = k, false);
    public static final Target CHANCE_DOWN = new Target("client:chanceDown", "Hit chance -",
            () -> Config.chanceDownKey, k -> Config.chanceDownKey = k, true);
    public static final Target CHANCE_UP = new Target("client:chanceUp", "Hit chance +",
            () -> Config.chanceUpKey, k -> Config.chanceUpKey = k, true);

    private Binds() {}

    public static Target forModule(Module m) {
        return new Target("mod:" + m.name, m.name, () -> m.key,
                k -> { m.key = k; Config.binds.put(Config.bindId(m.name), k); }, true);
    }

    public static List<Target> client() { return List.of(MENU, CHANCE_DOWN, CHANCE_UP); }

    private static List<Target> all() {
        List<Target> l = new ArrayList<>();
        for (Module m : ModuleManager.modules) l.add(forModule(m));
        l.addAll(client());
        return l;
    }

    /** Applies saved module binds over the defaults. Call after the modules are created. */
    public static void applySaved() {
        for (Module m : ModuleManager.modules) {
            Integer k = Config.binds.get(Config.bindId(m.name));
            if (k != null) m.key = k;
        }
    }

    /**
     * Binds key k (or -1 to clear) to t and saves. If another bind already uses k it is unbound, except the menu key,
     * which can never be taken away or cleared.
     */
    public static Result assign(Target t, int k) {
        if (k < 0) {
            if (!t.clearable()) return new Result(false, t.label() + " can't be unbound");
            t.set().accept(-1);
            Config.save();
            return new Result(true, t.label() + " unbound");
        }

        String moved = null;
        for (Target o : all()) {
            if (o.id().equals(t.id()) || o.get().getAsInt() != k) continue;
            if (!o.clearable()) return new Result(false, keyName(k) + " is the " + o.label().toLowerCase(Locale.ROOT));
            o.set().accept(-1);
            moved = o.label();
        }
        t.set().accept(k);
        Config.save();
        return new Result(true, t.label() + " = " + keyName(k) + (moved != null ? " (unbound " + moved + ")" : ""));
    }

    public static String keyName(int key) {
        if (key < 0) return "NONE";
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_ESCAPE: return "ESC";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACKSPACE";
            case GLFW.GLFW_KEY_INSERT: return "INSERT";
            case GLFW.GLFW_KEY_DELETE: return "DELETE";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_PAGE_UP: return "PAGE UP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PAGE DOWN";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS LOCK";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "L-ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "R-ALT";
            default: break;
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(key, 0);
        return n == null ? "KEY " + key : n.toUpperCase(Locale.ROOT);
    }
}
