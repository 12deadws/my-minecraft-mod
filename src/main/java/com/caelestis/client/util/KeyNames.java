package com.caelestis.client.util;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Human-readable names for GLFW key codes (used by the keybind field). */
public final class KeyNames {
    private static final Map<Integer, String> SPECIAL = new HashMap<>();

    static {
        SPECIAL.put(GLFW.GLFW_KEY_SPACE, "Space");
        SPECIAL.put(GLFW.GLFW_KEY_ENTER, "Enter");
        SPECIAL.put(GLFW.GLFW_KEY_TAB, "Tab");
        SPECIAL.put(GLFW.GLFW_KEY_BACKSPACE, "Backspace");
        SPECIAL.put(GLFW.GLFW_KEY_INSERT, "Insert");
        SPECIAL.put(GLFW.GLFW_KEY_DELETE, "Delete");
        SPECIAL.put(GLFW.GLFW_KEY_HOME, "Home");
        SPECIAL.put(GLFW.GLFW_KEY_END, "End");
        SPECIAL.put(GLFW.GLFW_KEY_PAGE_UP, "Page Up");
        SPECIAL.put(GLFW.GLFW_KEY_PAGE_DOWN, "Page Down");
        SPECIAL.put(GLFW.GLFW_KEY_UP, "Up");
        SPECIAL.put(GLFW.GLFW_KEY_DOWN, "Down");
        SPECIAL.put(GLFW.GLFW_KEY_LEFT, "Left");
        SPECIAL.put(GLFW.GLFW_KEY_RIGHT, "Right");
        SPECIAL.put(GLFW.GLFW_KEY_LEFT_SHIFT, "Left Shift");
        SPECIAL.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "Right Shift");
        SPECIAL.put(GLFW.GLFW_KEY_LEFT_CONTROL, "Left Ctrl");
        SPECIAL.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "Right Ctrl");
        SPECIAL.put(GLFW.GLFW_KEY_LEFT_ALT, "Left Alt");
        SPECIAL.put(GLFW.GLFW_KEY_RIGHT_ALT, "Right Alt");
        SPECIAL.put(GLFW.GLFW_KEY_CAPS_LOCK, "Caps Lock");
        for (int i = 0; i < 25; i++) SPECIAL.put(GLFW.GLFW_KEY_F1 + i, "F" + (i + 1));
        for (int i = 0; i < 10; i++) SPECIAL.put(GLFW.GLFW_KEY_KP_0 + i, "Numpad " + i);
    }

    private KeyNames() {
    }

    public static String name(int key) {
        if (key < 0) return "NONE";
        String s = SPECIAL.get(key);
        if (s != null) return s;
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null && !n.isEmpty()) return n.toUpperCase();
        return "Key " + key;
    }
}
