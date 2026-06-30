package com.goidacraft.goidachat.chat;

import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.MuteEntry;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.data.ViolationType;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import com.goidacraft.goidachat.util.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Эскалация наказаний за повторные нарушения автомодерации: считает нарушения игрока и
 * применяет мут возрастающей длительности по шкале из конфига. Счётчик сбрасывается через
 * настраиваемое время без нарушений. Состояние персистится между перезапусками.
 */
public final class EscalationManager {

    private static final Gson GSON = new Gson();

    private static final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private static Path file;

    private EscalationManager() {}

    static class State {
        int  count           = 0;
        long lastViolationAt = 0L;
    }

    public static void init(Path dataDir) {
        Path dir = dataDir.resolve("goidachat");
        file = dir.resolve("escalation.json");
        try {
            Files.createDirectories(dir);
            load();
        } catch (IOException ignored) {}
    }

    /**
     * Вызывается при каждом зафиксированном нарушении: инкрементирует счётчик и применяет
     * наказание по шкале эскалации (первое нарушение — только предупреждение).
     */
    public static void apply(ServerPlayer player, ViolationType type) {
        String key     = player.getUUID().toString();
        long   now     = System.currentTimeMillis();
        long   resetMs = (long) PluginConfig.escalationResetHours() * 3_600_000L;

        State state = states.computeIfAbsent(key, k -> new State());
        int count;
        synchronized (state) {
            if (now - state.lastViolationAt > resetMs) state.count = 0;
            state.count++;
            state.lastViolationAt = now;
            count = state.count;
        }
        saveAsync();

        List<String> durations = PluginConfig.escalationMuteDurations();
        if (durations.isEmpty() || count == 1) return;

        String durationStr = durations.get(Math.min(count - 2, durations.size() - 1));
        long expiresAt;
        try { expiresAt = TimeUtil.parseDuration(durationStr); }
        catch (IllegalArgumentException e) { return; }

        String name = player.getGameProfile().getName();
        PunishmentStorage.addMute(new MuteEntry(
                player.getUUID(), name,
                "[AutoMod] " + type.displayName,
                expiresAt, "AutoMod"));

        String timeStr = TimeUtil.formatRemaining(expiresAt);
        player.sendSystemMessage(ColorUtil.parse(
                "&cВы автоматически заглушены на &e" + timeStr
                + " &cза: &f" + type.displayName));

        MinecraftServer server = player.getServer();
        if (server != null && PluginConfig.escalationNotifyStaff()) {
            var msg = ColorUtil.parse("&8[&cAutoMod&8] &e" + name
                    + " &cзаглушён на &e" + timeStr
                    + " &8(&f" + type.displayName + "&8)");
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (LuckPermsUtil.hasPermission(p, "goidachat.mute", 2)) p.sendSystemMessage(msg);
            }
        }
    }

    /** Сбрасывает счётчик нарушений (например, при /violations clear). */
    public static void reset(UUID uuid) {
        states.remove(uuid.toString());
        saveAsync();
    }

    /** Текущий счётчик нарушений (0 если сброшен по таймауту). */
    public static int getCount(UUID uuid) {
        State state = states.get(uuid.toString());
        if (state == null) return 0;
        long resetMs = (long) PluginConfig.escalationResetHours() * 3_600_000L;
        if (System.currentTimeMillis() - state.lastViolationAt > resetMs) return 0;
        return state.count;
    }

    // ---------------------------------------------------------------- I/O

    private static void load() {
        try {
            if (!Files.exists(file)) return;
            Type type = new TypeToken<Map<String, State>>() {}.getType();
            Map<String, State> loaded = GSON.fromJson(Files.readString(file), type);
            if (loaded != null) states.putAll(loaded);
        } catch (Exception ignored) {}
    }

    private static void saveAsync() {
        Map<String, State> snapshot = new HashMap<>(states);
        String json = GSON.toJson(snapshot);
        CompletableFuture.runAsync(() -> {
            try { Files.writeString(file, json); } catch (IOException ignored) {}
        });
    }
}
