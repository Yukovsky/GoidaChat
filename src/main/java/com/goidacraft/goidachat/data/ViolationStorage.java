package com.goidacraft.goidachat.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public final class ViolationStorage {

    private static final Gson GSON = new Gson();
    private static final ConcurrentHashMap<String, ViolationEntry> map = new ConcurrentHashMap<>();
    private static Path file;

    private ViolationStorage() {}

    public static void init(Path dataDir) {
        Path dir = dataDir.resolve("goidachat");
        file = dir.resolve("violations.json");
        try {
            Files.createDirectories(dir);
            load();
        } catch (IOException ignored) {}
    }

    // ---------------------------------------------------------------- CRUD

    public static ViolationEntry add(UUID playerUuid, String playerName,
                                     ViolationType type, String details, String reason) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        ViolationEntry entry = new ViolationEntry(id, playerUuid, playerName, type, details, reason);
        map.put(id, entry);
        saveAsync();
        return entry;
    }

    public static ViolationEntry get(String id) { return map.get(id); }

    public static boolean update(String id, String newReason) {
        ViolationEntry entry = map.get(id);
        if (entry == null) return false;
        entry.reason = newReason;
        saveAsync();
        return true;
    }

    public static boolean delete(String id) {
        if (map.remove(id) == null) return false;
        saveAsync();
        return true;
    }

    // ---------------------------------------------------------------- List

    public static List<ViolationEntry> getAll() {
        List<ViolationEntry> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingLong((ViolationEntry e) -> e.timestamp).reversed());
        return list;
    }

    public static List<ViolationEntry> getByPlayerName(String name) {
        String lower = name.toLowerCase();
        return map.values().stream()
                .filter(e -> e.playerName.toLowerCase().equals(lower))
                .sorted(Comparator.comparingLong((ViolationEntry e) -> e.timestamp).reversed())
                .collect(Collectors.toList());
    }

    public static int clearByPlayer(UUID uuid) {
        int[] count = {0};
        map.entrySet().removeIf(e -> {
            if (e.getValue().playerUuid.equals(uuid)) { count[0]++; return true; }
            return false;
        });
        if (count[0] > 0) saveAsync();
        return count[0];
    }

    // ---------------------------------------------------------------- I/O

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            Type type = new TypeToken<List<ViolationEntry>>() {}.getType();
            List<ViolationEntry> list = GSON.fromJson(Files.readString(file), type);
            if (list == null) return;
            for (ViolationEntry e : list) map.put(e.id, e);
        } catch (Exception ignored) {}
    }

    private static void saveAsync() {
        String json = GSON.toJson(new ArrayList<>(map.values()));
        CompletableFuture.runAsync(() -> {
            try { Files.writeString(file, json); } catch (IOException ignored) {}
        });
    }
}
