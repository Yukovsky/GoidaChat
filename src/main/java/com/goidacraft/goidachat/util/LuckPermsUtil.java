package com.goidacraft.goidachat.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;

/**
 * Обёртка над LuckPerms API. Работает как с NeoForge-модом LuckPerms,
 * так и с Bukkit-плагином (общий API через Java ServiceLoader).
 *
 * Все вызовы перехватывают Exception, чтобы временная недоступность
 * (capability ещё не инициализирован при входе игрока) не ломала функции мода.
 */
public final class LuckPermsUtil {

    private static volatile LuckPerms LP = null;
    // true = LP точно не установлен (сервис не зарегистрирован ни разу)
    private static volatile boolean absent = false;

    private LuckPermsUtil() {}

    /**
     * Возвращает LP API или null, если LP недоступен.
     *
     * Ловит Throwable (а не только Exception), потому что когда LP установлен
     * как Bukkit-плагин (а не NeoForge-мод), его классы недоступны
     * в модульном ClassLoader NeoForge → JVM выбрасывает NoClassDefFoundError (Error, не Exception).
     * После первой ошибки absent=true — повторных попыток и повторных ошибок нет.
     */
    private static LuckPerms api() {
        if (LP != null) return LP;
        if (absent) return null;
        try {
            LuckPerms instance = LuckPermsProvider.get();
            LP = instance;
            return instance;
        } catch (Throwable t) {
            // NoClassDefFoundError если LP — Bukkit-плагин (классы не в NeoForge ClassLoader).
            // IllegalStateException если LP не загружен.
            absent = true;
        }
        return null;
    }

    /** Префикс ника: сначала LuckPerms, при пустом — косметика FTB Ranks (склейка рангов). */
    public static String getPrefix(ServerPlayer player) {
        String lp = meta(player, true);
        return !lp.isEmpty() ? lp : FtbRanksCosmetics.prefix(player);
    }

    /** Суффикс ника: сначала LuckPerms, при пустом — косметика FTB Ranks (склейка рангов). */
    public static String getSuffix(ServerPlayer player) {
        String lp = meta(player, false);
        return !lp.isEmpty() ? lp : FtbRanksCosmetics.suffix(player);
    }

    /** Возвращает префикс ({@code prefix=true}) или суффикс из метаданных LuckPerms, либо "". */
    private static String meta(ServerPlayer player, boolean prefix) {
        LuckPerms lp = api();
        if (lp == null) return "";
        try {
            User user = lp.getUserManager().getUser(player.getUUID());
            if (user == null) return "";
            String v = prefix ? user.getCachedData().getMetaData().getPrefix()
                              : user.getCachedData().getMetaData().getSuffix();
            return v != null ? v : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * Проверяет право игрока. Порядок: LuckPerms (если установлен) → FTB Ranks (если установлен)
     * → OP-уровень. Так на сервере с любым из этих менеджеров прав узел вида {@code goidachat.ban}
     * можно выдать рангу/игроку, а без них работает обычная проверка OP.
     */
    public static boolean hasPermission(ServerPlayer player, String node, int fallbackOpLevel) {
        LuckPerms lp = api();
        if (lp != null) {
            try {
                User user = lp.getUserManager().getUser(player.getUUID());
                if (user != null) {
                    return user.getCachedData().getPermissionData()
                            .checkPermission(node).asBoolean();
                }
            } catch (Throwable ignored) {
                // LP временно недоступен — пробуем дальше.
            }
        }
        // FTB Ranks (когда установлен вместо LuckPerms): берём значение узла из ранга игрока.
        java.util.Optional<Boolean> ftb = FtbRanksPermissions.check(player, node);
        if (ftb.isPresent()) return ftb.get();
        return player.hasPermissions(fallbackOpLevel);
    }
}
