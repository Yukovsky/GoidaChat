package com.goidacraft.goidachat.chat;

import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiSpamManager {

    private static final ConcurrentHashMap<UUID, Deque<Long>> timestamps = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String>      lastMsg    = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer>     dupCount   = new ConcurrentHashMap<>();

    private AntiSpamManager() {}

    private static boolean isBypass(ServerPlayer p) {
        return LuckPermsUtil.hasPermission(p, "goidachat.antispam.bypass", 2);
    }

    /** Флуд: слишком много сообщений за единицу времени. */
    public static boolean isSpamming(ServerPlayer player) {
        if (isBypass(player)) return false;

        long now = System.currentTimeMillis();
        int window = PluginConfig.spamWindowMs();
        int max    = PluginConfig.spamMaxMessages();

        Deque<Long> history = timestamps.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        synchronized (history) {
            history.removeIf(t -> now - t > window);
            if (history.size() >= max) return true;
            history.addLast(now);
        }
        return false;
    }

    /** Дубликат: одно и то же сообщение подряд слишком много раз. */
    public static boolean isDuplicate(ServerPlayer player, String message) {
        if (isBypass(player)) return false;
        int max = PluginConfig.spamDuplicateCount();
        if (max <= 0) return false;

        UUID uuid = player.getUUID();
        String normalized = message.trim().toLowerCase();
        String prev = lastMsg.get(uuid);

        if (normalized.equals(prev)) {
            int count = dupCount.merge(uuid, 1, Integer::sum);
            return count >= max;
        } else {
            lastMsg.put(uuid, normalized);
            dupCount.put(uuid, 1);
            return false;
        }
    }

    /** Символьный спам: один символ повторяется подряд больше max раз. */
    public static boolean isCharRepeat(ServerPlayer player, String message) {
        if (isBypass(player)) return false;
        if (!PluginConfig.charRepeatEnabled()) return false;
        int max = PluginConfig.charRepeatMax();
        if (max <= 0) return false;

        char prev  = 0;
        int  count = 0;
        for (char c : message.toCharArray()) {
            if (c == prev) {
                if (++count > max) return true;
            } else {
                prev  = c;
                count = 1;
            }
        }
        return false;
    }

    public static void remove(UUID uuid) {
        timestamps.remove(uuid);
        lastMsg.remove(uuid);
        dupCount.remove(uuid);
    }
}
