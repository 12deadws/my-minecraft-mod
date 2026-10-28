package com.caelestis.client.setting;

import com.google.gson.JsonElement;

/** A clickable action (not persisted). */
public class ButtonSetting extends Setting {
    private final Runnable action;

    public ButtonSetting(String name, String tooltip, Runnable action) {
        super(name, tooltip);
        this.action = action;
    }

    public void run() {
        action.run();
    }

    @Override
    public JsonElement save() {
        return null;
    }

    @Override
    public void load(JsonElement element) {
    }
}
