package com.goidacraft.goidachat.api;

import com.goidacraft.goidachat.data.MuteEntry;
import com.goidacraft.goidachat.data.PunishmentStorage;

import java.util.UUID;

/**
 * Стабильная публичная точка входа для других модов, которым нужно программно наложить/снять мут
 * через GoidaChat — например, GoidaJail глушит заключённого на время отсидки.
 *
 * <p>Полный мут ({@code voice=true}) глушит и текстовый чат (проверка в обработчике чата), и
 * голосовой (через {@code VoiceMuteHook} при наличии Simple Voice Chat). Мут хранится по UUID
 * (одна запись на игрока), поэтому потребителям дан {@link #unmuteIfBy} / {@link #isMutedBy} с
 * тегом источника, чтобы не затирать чужой мут (например, админский).
 *
 * <p>Потребитель должен вызывать эти методы только за guard'ом {@code ModList.isLoaded("goidachat")}
 * — тогда при отсутствии GoidaChat класс не разрешается и не возникает {@code NoClassDefFoundError}.
 */
public final class MuteApi {

    private MuteApi() {}

    /** Признак доступности (класс присутствует ⇒ GoidaChat установлен). */
    public static boolean isAvailable() {
        return true;
    }

    /**
     * Вечный мут (до явного снятия). Подходит для состояний, чьё «время» не совпадает со стенными
     * часами — например, тюрьма GoidaJail, где срок не идёт, пока игрок оффлайн.
     *
     * @param voice если {@code true} — глушит ещё и голосовой чат, а не только текст.
     */
    public static void mutePermanent(UUID uuid, String playerName, String reason, String mutedBy, boolean voice) {
        if (uuid == null) return;
        PunishmentStorage.addMute(new MuteEntry(uuid, playerName, reason, Long.MAX_VALUE, mutedBy, voice));
    }

    /** Срочный мут до момента {@code expiresAt} (epoch-ms). {@code voice=true} — плюс голос. */
    public static void mute(UUID uuid, String playerName, String reason, long expiresAt, String mutedBy, boolean voice) {
        if (uuid == null) return;
        PunishmentStorage.addMute(new MuteEntry(uuid, playerName, reason, expiresAt, mutedBy, voice));
    }

    /** Безусловно снять мут игрока. */
    public static void unmute(UUID uuid) {
        if (uuid != null) PunishmentStorage.removeMute(uuid);
    }

    /**
     * Снять мут, только если он наложен источником {@code mutedBy}. Не даёт одной подсистеме снять
     * чужой мут (например, админский), заменивший её собственную запись.
     *
     * @return {@code true}, если совпадающий мут был снят.
     */
    public static boolean unmuteIfBy(UUID uuid, String mutedBy) {
        if (uuid == null || mutedBy == null) return false;
        MuteEntry m = PunishmentStorage.getMute(uuid);
        if (m != null && mutedBy.equals(m.mutedBy)) {
            PunishmentStorage.removeMute(uuid);
            return true;
        }
        return false;
    }

    /** @return {@code true}, если у игрока есть активный мут от источника {@code mutedBy}. */
    public static boolean isMutedBy(UUID uuid, String mutedBy) {
        if (uuid == null || mutedBy == null) return false;
        MuteEntry m = PunishmentStorage.getMute(uuid);
        return m != null && mutedBy.equals(m.mutedBy);
    }
}
