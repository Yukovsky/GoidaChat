package com.goidacraft.goidachat.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class IgnoreStorage {

    private static final Gson GSON = new Gson();
    private static final ConcurrentHashMap<UUID, IgnoreData> data = new ConcurrentHashMap<>();
    private static Path file;

    private IgnoreStorage() {}

    public static void init(Path dataDir) {
        file = dataDir.resolve("goidachat").resolve("ignores.json");
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                Type type = new TypeToken<Map<String, IgnoreData>>() {}.getType();
                Map<String, IgnoreData> raw = GSON.fromJson(Files.readString(file), type);
                if (raw != null) raw.forEach((k, v) -> data.put(UUID.fromString(k), v));
            }
        } catch (IOException ignored) {}
    }

    public static void addIgnore(UUID source, UUID target, boolean all) {
        IgnoreData d = data.computeIfAbsent(source, k -> new IgnoreData());
        if (all) {
            d.allIgnored.add(target);
            d.pmIgnored.remove(target);   // pm-only → all: убираем из pm
        } else {
            d.pmIgnored.add(target);
            d.allIgnored.remove(target);  // all → pm-only: убираем из all
        }
        saveAsync();
    }

    public static void removeIgnore(UUID source, UUID target) {
        IgnoreData d = data.get(source);
        if (d != null) { d.allIgnored.remove(target); d.pmIgnored.remove(target); saveAsync(); }
    }

    public static boolean isIgnoredAll(UUID source, UUID target) {
        IgnoreData d = data.get(source);
        return d != null && d.allIgnored.contains(target);
    }

    public static boolean isIgnoredPm(UUID source, UUID target) {
        IgnoreData d = data.get(source);
        return d != null && (d.allIgnored.contains(target) || d.pmIgnored.contains(target));
    }

    private static void saveAsync() {
        Map<String, IgnoreData> raw = new HashMap<>();
        data.forEach((k, v) -> raw.put(k.toString(), v));
        String json = GSON.toJson(raw);
        CompletableFuture.runAsync(() -> {
            try { Files.writeString(file, json); } catch (IOException ignored) {}
        });
    }
}
