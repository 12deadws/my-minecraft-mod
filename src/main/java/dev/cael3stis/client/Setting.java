package dev.cael3stis.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Typed settings shared by all modules and persisted independently. */
public abstract class Setting {
    public final String name;
    protected Setting(String name) { this.name = name; }
    public abstract JsonElement json();
    public abstract void read(JsonElement element);
    public abstract String display();

    public static final class Toggle extends Setting {
        public boolean value;
        public Toggle(String name, boolean def) { super(name); value = def; }
        public JsonElement json() { return new JsonPrimitive(value); }
        public void read(JsonElement e) { if (e.isJsonPrimitive()) value = e.getAsBoolean(); }
        public String display() { return value ? "ON" : "OFF"; }
    }
    public static final class Number extends Setting {
        public final int min, max, step;
        public int value;
        public Number(String name, int def, int min, int max, int step) {
            super(name); this.min=min; this.max=max; this.step=step; value=Math.clamp(def,min,max);
        }
        public JsonElement json() { return new JsonPrimitive(value); }
        public void read(JsonElement e) { if(e.isJsonPrimitive()) value=Math.clamp(e.getAsInt(),min,max); }
        public String display() { return String.valueOf(value); }
        public void adjust(int dir) { value=Math.clamp(value+step*dir,min,max); }
    }
    public static final class Choice extends Setting {
        public final String[] options;
        public int index;
        public Choice(String name, int def, String... options) {
            super(name); this.options=options; index=Math.clamp(def,0,options.length-1);
        }
        public JsonElement json() { return new JsonPrimitive(options[index]); }
        public void read(JsonElement e) { if(e.isJsonPrimitive()) for(int i=0;i<options.length;i++) if(options[i].equalsIgnoreCase(e.getAsString())) index=i; }
        public void next(int dir) { index=Math.floorMod(index+dir,options.length); }
        public String display() { return options[index]; }
        public boolean is(String value) { return display().equals(value); }
    }
}
