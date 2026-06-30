package com.goidacraft.goidachat.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BanAttemptStorage {

    private static final int  MAX_ENTRIES = 500;
    private static final Gson GSON        = new Gson();

    private static final CopyOnWriteArrayList<BanAttemptEntry> entries = new CopyOnWriteArrayList<>();
    private static Path attemptsFile;

    private BanAttemptStorage() {}

    public static void init(Path dataDir) {
        Path dir = dataDir.resolve("goidachat");
        attemptsFile = dir.resolve("ban_attempts.json");
        try {
            Files.createDirectories(dir);
            load();
        } catch (IOException ignored) {}
    }

    public static void log(String evaderName, String evaderUuid,
                           String ip, String hwid, String via, String bannedPlayer) {
        entries.add(new BanAttemptEntry(evaderName, evaderUuid, ip, hwid, via, bannedPlayer));
        if (entries.size() > MAX_ENTRIES) entries.remove(0);
        saveAsync();
    }

    /** Возвращает все записи от новых к старым. */
    public static List<BanAttemptEntry> getAll() {
        List<BanAttemptEntry> copy = new ArrayList<>(entries);
        Collections.reverse(copy);
        return copy;
    }

    public static int size() { return entries.size(); }

    private static void load() {
        try {
            if (!Files.exists(attemptsFile)) return;
            Type type = new TypeToken<List<BanAttemptEntry>>() {}.getType();
            List<BanAttemptEntry> list = GSON.fromJson(Files.readString(attemptsFile), type);
            if (list != null) entries.addAll(list);
        } catch (Exception ignored) {}
    }

    private static void saveAsync() {
        List<BanAttemptEntry> snapshot = new ArrayList<>(entries);
        String json = GSON.toJson(snapshot);
        CompletableFuture.runAsync(() -> {
            try { Files.writeString(attemptsFile, json); } catch (IOException ignored) {}
        });
    }
}
