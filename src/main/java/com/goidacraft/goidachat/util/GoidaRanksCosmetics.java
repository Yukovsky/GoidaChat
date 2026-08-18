package com.goidacraft.goidachat.util;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/**
 * Читает итоговый префикс/суффикс игрока из мода <b>GoidaRanks</b> (его {@code GoidaRanksAPI}), если
 * тот установлен. GoidaRanks — единый источник косметики: он разрешает префикс и суффикс независимо
 * по весам поверх рангов FTB и персональных оверрайдов. Поэтому в чате он должен иметь приоритет над
 * прямым чтением LuckPerms/FTB Ranks.
 *
 * <p>Через рефлексию — жёсткой зависимости от GoidaRanks нет (как в {@link FtbRanksCosmetics}). Если
 * мод не установлен или API недоступен, возвращает пустые строки, и вызывающий код откатывается на
 * обычную логику (LuckPerms → FTB Ranks).
 */
public final class GoidaRanksCosmetics {

    private static volatile boolean absent = false;
    private static volatile Method prefixM; // GoidaRanksAPI.prefix(ServerPlayer) -> String [static]
    private static volatile Method suffixM; // GoidaRanksAPI.suffix(ServerPlayer) -> String [static]

    private GoidaRanksCosmetics() {}

    private static boolean init() {
        if (prefixM != null) return true;
        if (absent) return false;
        try {
            Class<?> api = Class.forName("com.goidacraft.goidaranks.api.GoidaRanksAPI");
            suffixM = api.getMethod("suffix", ServerPlayer.class);
            prefixM = api.getMethod("prefix", ServerPlayer.class); // последним: признак готовности
            return true;
        } catch (Throwable t) {
            absent = true;
            return false;
        }
    }

    public static String prefix(ServerPlayer player) {
        String v = invoke(true, player);
        return v.isEmpty() ? v : v + " ";
    }

    public static String suffix(ServerPlayer player) {
        String v = invoke(false, player);
        return v.isEmpty() ? v : " " + v;
    }

    private static String invoke(boolean prefix, ServerPlayer player) {
        if (player == null || !init()) return "";
        try {
            Object v = (prefix ? prefixM : suffixM).invoke(null, player);
            return v instanceof String s ? s : "";
        } catch (Throwable ignored) {
            return "";
        }
    }
}
