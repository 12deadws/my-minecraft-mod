package com.caelestis.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting {
    public final double min, max, step;
    public final String suffix;
    private volatile double value;

    public NumberSetting(String name, String tooltip, double def, double min, double max, double step) {
        this(name, tooltip, def, min, max, step, "");
    }

    public NumberSetting(String name, String tooltip, double def, double min, double max, double step, String suffix) {
        super(name, tooltip);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.value = clamp(def);
    }

    private double clamp(double v) {
        if (step > 0) v = Math.round(v / step) * step;
        return Math.max(min, Math.min(max, v));
    }

    public double get() {
        return value;
    }

    public int getInt() {
        return (int) Math.round(value);
    }

    public float getFloat() {
        return (float) value;
    }

    public void set(double v) {
        this.value = clamp(v);
    }

    /** Set from a 0..1 slider fraction. */
    public void setFraction(double f) {
        set(min + (max - min) * Math.max(0, Math.min(1, f)));
    }

    public double fraction() {
        return (value - min) / (max - min);
    }

    public String display() {
        if (step >= 1 || value == Math.rint(value)) return (long) Math.rint(value) + suffix;
        return String.format("%.1f%s", value, suffix);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement e) {
        try {
            if (e != null && e.isJsonPrimitive()) set(e.getAsDouble());
        } catch (Exception ignored) {
        }
    }
}
