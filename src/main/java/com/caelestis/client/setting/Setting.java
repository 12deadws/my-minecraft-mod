package com.caelestis.client.setting;

import com.google.gson.JsonElement;

/** Base class for every configurable value shown in the ClickGUI. */
public abstract class Setting {
    public final String name;
    public final String tooltip;

    protected Setting(String name, String tooltip) {
        this.name = name;
        this.tooltip = tooltip;
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement element);
}
