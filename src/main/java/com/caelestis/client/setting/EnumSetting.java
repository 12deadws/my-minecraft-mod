package com.caelestis.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A choice from a fixed list of string options (shown as a dropdown in the GUI). */
public class EnumSetting extends Setting {
    public final String[] options;
    private volatile int index;

    public EnumSetting(String name, String tooltip, String def, String... options) {
        super(name, tooltip);
        this.options = options;
        this.index = 0;
        set(def);
    }

    public String get() {
        return options[index];
    }

    public boolean is(String option) {
        return options[index].equalsIgnoreCase(option);
    }

    public int index() {
        return index;
    }

    public void setIndex(int i) {
        if (i >= 0 && i < options.length) index = i;
    }

    public void set(String option) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(option)) {
                index = i;
                return;
            }
        }
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(get());
    }

    @Override
    public void load(JsonElement e) {
        try {
            if (e != null && e.isJsonPrimitive()) set(e.getAsString());
        } catch (Exception ignored) {
        }
    }
}
