package com.goidacraft.goidachat.util;

import com.goidacraft.goidachat.config.PluginConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * Проверяет вейнш-статус игрока через мод <b>Vanishmod</b> (modId {@code vmod}), если тот установлен.
 * Через рефлексию — жёсткой зависимости от Vanishmod нет, как в {@link GoidaRanksCosmetics}. Если мод
 * не установлен или фича выключена в конфиге ({@link PluginConfig#vanishHideEnabled()}), {@link #isVanished}
 * всегда возвращает {@code false} — это единая точка отключения всей фичи (блок ЛС, маскировка ника
 * в чате), все места ниже по цепочке уже проверяют именно её.
 */
public final class VanishCompat {

    /** Длина заглушки фиксированная и не зависит от длины настоящего ника — иначе по длине текста
     * в чате всё равно можно догадаться, какой именно игрок сейчас в вейнше. */
    private static final int MASK_LENGTH = 10;

    private static volatile boolean absent = false;
    private static volatile Method isVanishedM; // VanishUtil.isVanished(Player) -> boolean [static]
    private static volatile Method isVanishedObserverM; // VanishUtil.isVanished(Player, Entity) -> boolean [static]

    private VanishCompat() {}

    private static boolean init() {
        if (isVanishedM != null) return true;
        if (absent) return false;
        try {
            Class<?> util = Class.forName("redstonedubstep.mods.vanishmod.VanishUtil");
            isVanishedM = util.getMethod("isVanished", Player.class);
            try {
                isVanishedObserverM = util.getMethod("isVanished", Player.class, net.minecraft.world.entity.Entity.class);
            } catch (Throwable ignored) {}
            return true;
        } catch (Throwable t) {
            absent = true;
            return false;
        }
    }

    public static boolean isVanished(Player player) {
        if (player == null || !PluginConfig.vanishHideEnabled() || !init()) return false;
        try {
            Object v = isVanishedM.invoke(null, player);
            return v instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Проверяет, скрыт ли игрок от конкретного наблюдателя. Если наблюдатель имеет право видеть
     * вейнш-игроков (например, админ), метод вернёт {@code false}.
     */
    public static boolean isVanished(Player player, net.minecraft.world.entity.Entity observer) {
        if (player == null || !PluginConfig.vanishHideEnabled() || !init()) return false;
        if (observer == null) return isVanished(player);
        if (isVanishedObserverM != null) {
            try {
                Object v = isVanishedObserverM.invoke(null, player, observer);
                return v instanceof Boolean b && b;
            } catch (Throwable ignored) {}
        }
        return isVanished(player);
    }

    /** Заглушка фиксированной длины для маскировки ника. */
    public static String maskedNickname() {
        return "X".repeat(MASK_LENGTH);
    }

    /**
     * Ник игрока для показа в чате: обычный текст, либо, если игрок в Vanish — заглушка
     * фиксированной длины с кодом форматирования {@code &k} (obfuscated).
     */
    public static String displayName(ServerPlayer player) {
        return displayName(player, null);
    }

    public static String displayName(ServerPlayer player, ServerPlayer observer) {
        String name = player.getGameProfile().getName();
        return isVanished(player, observer) ? "&k" + maskedNickname() : name;
    }
}
