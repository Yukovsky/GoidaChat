package com.goidacraft.goidachat.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public final class PunishmentStorage {

    private static final Gson GSON = new Gson();

    private static final ConcurrentHashMap<UUID,   BanEntry>  bansByUuid  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, BanEntry>  bansByIp    = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, BanEntry>  bansByHwid  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID,   MuteEntry> mutesByUuid = new ConcurrentHashMap<>();

    private static Path bansFile;
    private static Path mutesFile;

    private PunishmentStorage() {}

    public static void init(Path dataDir) {
        Path dir = dataDir.resolve("goidachat");
        bansFile  = dir.resolve("bans.json");
        mutesFile = dir.resolve("mutes.json");
        try {
            Files.createDirectories(dir);
            loadBans();
            loadMutes();
        } catch (IOException ignored) {}
    }

    // ---------------------------------------------------------------- bans

    public static BanEntry getBanByUuid(UUID uuid) {
        BanEntry e = bansByUuid.get(uuid);
        if (e != null && e.isExpired()) { bansByUuid.remove(uuid); saveBansAsync(); return null; }
        return e;
    }

    public static BanEntry getBanByIp(String ip) {
        if (ip == null) return null;
        BanEntry e = bansByIp.get(ip);
        if (e != null && e.isExpired()) { bansByIp.remove(ip); saveBansAsync(); return null; }
        return e;
    }

    public static BanEntry getBanByHwid(String hwid) {
        if (hwid == null) return null;
        BanEntry e = bansByHwid.get(hwid);
        if (e != null && e.isExpired()) { bansByHwid.remove(hwid); saveBansAsync(); return null; }
        return e;
    }

    public static void addBans(List<BanEntry> entries) {
        for (BanEntry e : entries) {
            switch (e.type) {
                case UUID -> bansByUuid.put(java.util.UUID.fromString(e.target), e);
                case IP   -> bansByIp.put(e.target, e);
                case HWID -> bansByHwid.put(e.target, e);
            }
        }
        saveBansAsync();
    }

    public static int removeGroup(String banId) {
        int count = 0;
        count += removeFromMap(bansByUuid, banId);
        count += removeFromMap(bansByIp,   banId);
        count += removeFromMap(bansByHwid, banId);
        if (count > 0) saveBansAsync();
        return count;
    }

    private static <K> int removeFromMap(ConcurrentHashMap<K, BanEntry> map, String banId) {
        int[] count = {0};
        map.entrySet().removeIf(e -> { if (banId.equals(e.getValue().banId)) { count[0]++; return true; } return false; });
        return count[0];
    }

    // ---------------------------------------------------------------- mutes

    public static MuteEntry getMute(UUID uuid) {
        MuteEntry e = mutesByUuid.get(uuid);
        if (e != null && e.isExpired()) { mutesByUuid.remove(uuid); saveMutesAsync(); return null; }
        return e;
    }

    public static void addMute(MuteEntry entry) {
        mutesByUuid.put(entry.uuid, entry);
        saveMutesAsync();
    }

    public static void removeMute(UUID uuid) {
        if (mutesByUuid.remove(uuid) != null) saveMutesAsync();
    }

    // ---------------------------------------------------------------- housekeeping

    public static List<BanEntry> getAllUuidBans() {
        bansByUuid.entrySet().removeIf(e -> e.getValue().isExpired());
        List<BanEntry> list = new ArrayList<>(bansByUuid.values());
        list.sort(Comparator.comparingLong((BanEntry e) -> e.createdAt).reversed());
        return list;
    }

    public static List<BanType> getTypesForBanId(String banId) {
        List<BanType> types = new ArrayList<>();
        if (bansByUuid.values().stream().anyMatch(e -> banId.equals(e.banId))) types.add(BanType.UUID);
        if (bansByIp.values().stream().anyMatch(e -> banId.equals(e.banId)))   types.add(BanType.IP);
        if (bansByHwid.values().stream().anyMatch(e -> banId.equals(e.banId))) types.add(BanType.HWID);
        return types;
    }

    /** Возвращает все записи (UUID/IP/HWID) для одного banId. */
    public static List<BanEntry> getBansByBanId(String banId) {
        List<BanEntry> result = new ArrayList<>();
        bansByUuid.values().stream().filter(e -> banId.equals(e.banId)).findFirst().ifPresent(result::add);
        bansByIp.values().stream().filter(e -> banId.equals(e.banId)).findFirst().ifPresent(result::add);
        bansByHwid.values().stream().filter(e -> banId.equals(e.banId)).findFirst().ifPresent(result::add);
        return result;
    }

    public static void purgeExpired() {
        bansByUuid.entrySet().removeIf(e -> e.getValue().isExpired());
        bansByIp.entrySet().removeIf(e -> e.getValue().isExpired());
        bansByHwid.entrySet().removeIf(e -> e.getValue().isExpired());
        mutesByUuid.entrySet().removeIf(e -> e.getValue().isExpired());
        saveBansAsync();
        saveMutesAsync();
    }

    // ---------------------------------------------------------------- I/O

    private static void loadBans() {
        try {
            if (!Files.exists(bansFile)) return;
            Type type = new TypeToken<List<BanEntry>>() {}.getType();
            List<BanEntry> list = GSON.fromJson(Files.readString(bansFile), type);
            if (list == null) return;
            for (BanEntry e : list) {
                if (e == null || e.type == null || e.target == null || e.isExpired()) continue;
                switch (e.type) {
                    case UUID -> bansByUuid.put(java.util.UUID.fromString(e.target), e);
                    case IP   -> bansByIp.put(e.target, e);
                    case HWID -> bansByHwid.put(e.target, e);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void loadMutes() {
        try {
            if (!Files.exists(mutesFile)) return;
            Type type = new TypeToken<List<MuteEntry>>() {}.getType();
            List<MuteEntry> list = GSON.fromJson(Files.readString(mutesFile), type);
            if (list == null) return;
            for (MuteEntry e : list) {
                if (e != null && e.uuid != null && !e.isExpired()) mutesByUuid.put(e.uuid, e);
            }
        } catch (Exception ignored) {}
    }

    private static void saveBansAsync() {
        List<BanEntry> all = new ArrayList<>();
        all.addAll(bansByUuid.values());
        all.addAll(bansByIp.values());
        all.addAll(bansByHwid.values());
        String json = GSON.toJson(all);
        CompletableFuture.runAsync(() -> {
            try { Files.writeString(bansFile, json); } catch (IOException ignored) {}
        });
    }

    private static void saveMutesAsync() {
        String json = GSON.toJson(new ArrayList<>(mutesByUuid.values()));
        CompletableFuture.runAsync(() -> {
            try { Files.writeString(mutesFile, json); } catch (IOException ignored) {}
        });
    }
}
