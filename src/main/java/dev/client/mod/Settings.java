package dev.client.mod;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Builders for the +/- rows in a module's dropdown. */
public final class Settings {
    private Settings() {}

    /** Steps through a list of named options. */
    public static Setting cycle(String name, String[] options, IntSupplier get, IntConsumer set) {
        return new Setting(name, () -> options[get.getAsInt()],
                () -> set.accept((get.getAsInt() + options.length - 1) % options.length),
                () -> set.accept((get.getAsInt() + 1) % options.length));
    }

    /** Integer with min/max/step and an optional unit suffix. */
    public static Setting intRange(String name, int min, int max, int step, String suffix, IntSupplier get, IntConsumer set) {
        return new Setting(name, () -> get.getAsInt() + suffix,
                () -> set.accept(Math.max(min, get.getAsInt() - step)),
                () -> set.accept(Math.min(max, get.getAsInt() + step)));
    }
}
