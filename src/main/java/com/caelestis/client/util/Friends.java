package com.caelestis.client.util;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/** Friend list (lower-cased player names). Friends are never targeted by any module. */
public final class Friends {
    private static final Set<String> NAMES = ConcurrentHashMap.newKeySet();

    private Friends() {
    }

    public static boolean isFriend(String name) {
        return name != null && NAMES.contains(name.toLowerCase());
    }

    public static void add(String name) {
        if (name != null && !name.isBlank()) NAMES.add(name.toLowerCase());
    }

    public static void remove(String name) {
        if (name != null) NAMES.remove(name.toLowerCase());
    }

    public static void clear() {
        NAMES.clear();
    }

    public static int size() {
        return NAMES.size();
    }

    public static Set<String> snapshot() {
        return Collections.unmodifiableSet(new TreeSet<>(NAMES));
    }
}
