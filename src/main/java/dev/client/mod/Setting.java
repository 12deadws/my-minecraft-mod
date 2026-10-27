package dev.client.mod;

import java.util.function.Supplier;

/** One row in a module's dropdown. If dec/inc are null it is shown as read-only info. */
public class Setting {
    public final String name;
    public final Supplier<String> value;
    public final Runnable dec, inc;

    public Setting(String name, Supplier<String> value, Runnable dec, Runnable inc) {
        this.name = name; this.value = value; this.dec = dec; this.inc = inc;
    }

    public static Setting info(String name, Supplier<String> value) {
        return new Setting(name, value, null, null);
    }

    public boolean adjustable() { return dec != null && inc != null; }
}
