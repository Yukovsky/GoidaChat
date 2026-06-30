package com.goidacraft.goidachat.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TrustedAccounts {

    private static final Gson GSON = new Gson();
    private static final Set<UUID> trusted = ConcurrentHashMap.newKeySet();
    private static Path file;

    private TrustedAccounts() {}

    public static void init(Path dataDir) {
        Path dir = dataDir.resolve("goidachat");
        file = dir.resolve("trusted.json");
        try {
            Files.createDirectories(dir);
            load();
        } catch (IOException ignored) {}
    }

    public static boolean contains(UUID uuid) { return trusted.contains(uuid); }

    public static boolean add(UUID uuid) {
        boolean added = trusted.add(uuid);
        if (added) saveAsync();
        return added;
    }

    public static boolean remove(UUID uuid) {
        boolean removed = trusted.remove(uuid);
        if (removed) saveAsync();
        return removed;
    }

    private static void load() {
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> list = GSON.fromJson(json, type);
            if (list != null) list.forEach(s -> trusted.add(UUID.fromString(s)));
        } catch (IOException | IllegalArgumentException ignored) {}
    }

    private static void saveAsync() {
        List<String> snapshot = trusted.stream().map(UUID::toString).toList();
        Thread t = new Thread(() -> {
            try { Files.writeString(file, GSON.toJson(snapshot)); } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }
}
