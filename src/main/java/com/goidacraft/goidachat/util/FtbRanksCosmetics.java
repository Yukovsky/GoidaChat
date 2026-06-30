package com.goidacraft.goidachat.util;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Читает косметику оформления ника из FTB Ranks и <b>складывает</b> форматы всех рангов игрока в
 * единые префикс/суффикс — чтобы показывались сразу и донат-звезда, и клан-тег.
 *
 * <p>Сам FTB Ranks показывает {@code ftbranks.name_format} только одного ранга — с наибольшим
 * {@code power}. Здесь мы перебираем все ранги игрока ({@code FTBRanksAPI.manager().getRanks},
 * порядок — по убыванию power) и для каждого берём узел {@code ftbranks.name_format}: часть строки
 * до {@code {name}} идёт в префикс, часть после — в суффикс. Так «&3✦&r {name}» (донат) и
 * «{name} &b[WC]&r» (клан) вместе дают «✦ ник [WC]».
 *
 * <p>Всё через рефлексию — жёсткой зависимости от FTB Ranks нет, как и в {@link FtbRanksPermissions}.
 * При отсутствии мода или пустом результате возвращает пустые строки, и вызывающий код использует
 * своё обычное оформление (LuckPerms / без префикса).
 */
public final class FtbRanksCosmetics {

    private static final String NAME_FORMAT_NODE = "ftbranks.name_format";
    private static final String PLACEHOLDER = "{name}";
    private static final String[] EMPTY = {"", ""};

    // true = FTB Ranks точно отсутствует (классы не загрузились ни разу) — повторных попыток нет.
    private static volatile boolean absent = false;
    private static volatile Method managerM;        // FTBRanksAPI.manager() [static]
    private static volatile Method getRanksM;        // RankManager.getRanks(ServerPlayer)
    private static volatile Method getPermissionM;    // Rank.getPermission(String)
    private static volatile Method isEmptyM;          // PermissionValue.isEmpty()
    private static volatile Method asStringM;         // PermissionValue.asString() -> Optional<String>

    private FtbRanksCosmetics() {}

    private static boolean init() {
        if (managerM != null) return true;
        if (absent) return false;
        try {
            Class<?> api = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            Class<?> rankManager = Class.forName("dev.ftb.mods.ftbranks.api.RankManager");
            Class<?> rank = Class.forName("dev.ftb.mods.ftbranks.api.Rank");
            Class<?> value = Class.forName("dev.ftb.mods.ftbranks.api.PermissionValue");
            getRanksM = rankManager.getMethod("getRanks", ServerPlayer.class);
            getPermissionM = rank.getMethod("getPermission", String.class);
            isEmptyM = value.getMethod("isEmpty");
            asStringM = value.getMethod("asString");
            managerM = api.getMethod("manager"); // ставим последним: признак готовности
            return true;
        } catch (Throwable t) {
            // ClassNotFound / NoClassDefFound — мод не установлен. Больше не пробуем.
            absent = true;
            return false;
        }
    }

    /** Префикс (часть до {@code {name}}), собранный из всех рангов игрока. "" если FTB Ranks нет. */
    public static String prefix(ServerPlayer player) {
        return compute(player)[0];
    }

    /** Суффикс (часть после {@code {name}}), собранный из всех рангов игрока. */
    public static String suffix(ServerPlayer player) {
        return compute(player)[1];
    }

    @SuppressWarnings("unchecked")
    private static String[] compute(ServerPlayer player) {
        if (player == null || !init()) return EMPTY;
        try {
            Object manager = managerM.invoke(null);
            if (manager == null) return EMPTY;
            List<?> ranks = (List<?>) getRanksM.invoke(manager, player);
            if (ranks == null || ranks.isEmpty()) return EMPTY;

            StringBuilder prefix = new StringBuilder();
            StringBuilder suffix = new StringBuilder();
            for (Object rank : ranks) { // порядок: power по убыванию
                Object pv = getPermissionM.invoke(rank, NAME_FORMAT_NODE);
                if (pv == null || Boolean.TRUE.equals(isEmptyM.invoke(pv))) continue;
                Optional<String> fmtOpt = (Optional<String>) asStringM.invoke(pv);
                String fmt = fmtOpt != null ? fmtOpt.orElse(null) : null;
                if (fmt == null || fmt.isEmpty()) continue;

                int i = fmt.indexOf(PLACEHOLDER);
                if (i < 0) {
                    prefix.append(fmt); // формат без {name} — считаем целиком префиксом
                } else {
                    prefix.append(fmt, 0, i);
                    suffix.append(fmt, i + PLACEHOLDER.length(), fmt.length());
                }
            }
            if (prefix.length() == 0 && suffix.length() == 0) return EMPTY;
            return new String[]{prefix.toString(), suffix.toString()};
        } catch (Throwable ignored) {
            return EMPTY;
        }
    }
}
