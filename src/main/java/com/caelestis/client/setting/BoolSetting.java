package com.caelestis.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting {
    private volatile boolean value;

    public BoolSetting(String name, String tooltip, boolean def) {
        super(name, tooltip);
        this.value = def;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean v) {
        this.value = v;
    }

    public void toggle() {
        this.value = !this.value;
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        try {
            if (e != null && e.isJsonPrimitive()) value = e.getAsBoolean();
        } catch (Exception ignored) {
        }
    }
}
