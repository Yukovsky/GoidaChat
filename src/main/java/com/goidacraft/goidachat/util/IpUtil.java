package com.goidacraft.goidachat.util;

import net.minecraft.server.level.ServerPlayer;

/** Извлечение IP-адреса игрока без порта. */
public final class IpUtil {

    private IpUtil() {}

    public static String getIp(ServerPlayer player) {
        try {
            String ip = player.getIpAddress();
            if (ip != null && !ip.isBlank()) return clean(ip);
        } catch (Throwable ignored) {}
        return null;
    }

    /** Убирает префикс "/" и порт ":xxxxx" из строкового представления адреса. */
    private static String clean(String raw) {
        String s = raw;
        int slash = s.lastIndexOf('/');
        if (slash >= 0) s = s.substring(slash + 1);
        int colon = s.lastIndexOf(':');
        // отрезаем порт только если двоеточие одно (т.е. это не «голый» IPv6)
        if (colon >= 0 && s.indexOf(':') == colon) s = s.substring(0, colon);
        return s;
    }
}
