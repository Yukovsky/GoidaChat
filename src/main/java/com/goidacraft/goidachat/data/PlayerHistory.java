package com.goidacraft.goidachat.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class PlayerHistory {

    private static final Gson GSON = new Gson();
    private static final ConcurrentHashMap<UUID, PlayerRecord> byUuid = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, UUID> byName = new ConcurrentHashMap<>();
    private static Path file;

    private PlayerHistory() {}

    public static void init(Path dataDir) {
        file = dataDir.resolve("goidachat").resolve("history.json");
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Type type = new TypeToken<List<PlayerRecord>>() {}.getType();
                List<PlayerRecord> list = GSON.fromJson(json, type);
                if (list != null) {
                    for (PlayerRecord r : list) {
                        if (r.uuid != null) {
                            byUuid.put(r.uuid, r);
                            if (r.name != null) byName.put(r.name.toLowerCase(), r.uuid);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // start fresh
        }
    }

    public static void update(UUID uuid, String name, String ip, String hwid) {
        byUuid.compute(uuid, (k, existing) -> {
            if (existing == null) return new PlayerRecord(uuid, name, ip, hwid);
            existing.name     = name;
            existing.lastSeen = System.currentTimeMillis();
            if (ip   != null) existing.lastIp   = ip;
            if (hwid != null) existing.lastHwid = hwid;
            return existing;
        });
        if (name != null) byName.put(name.toLowerCase(), uuid);
        saveAsync();
    }

    public static PlayerRecord get(UUID uuid)      { return byUuid.get(uuid); }
    public static UUID getUuidByName(String name)  { return name == null ? null : byName.get(name.toLowerCase()); }

    private static void saveAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(file, GSON.toJson(new ArrayList<>(byUuid.values())));
            } catch (IOException ignored) {}
        });
    }
}
