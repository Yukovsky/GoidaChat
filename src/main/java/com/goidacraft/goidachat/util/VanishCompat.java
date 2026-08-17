package com.goidacraft.goidachat.util;

import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * Проверяет вейнш-статус игрока через мод <b>Vanishmod</b> (modId {@code vmod}), если тот установлен.
 * Через рефлексию — жёсткой зависимости от Vanishmod нет, как в {@link GoidaRanksCosmetics}. Если мод
 * не установлен, {@link #isVanished} всегда возвращает {@code false}.
 */
public final class VanishCompat {

    private static volatile boolean absent = false;
    private static volatile Method isVanishedM; // VanishUtil.isVanished(Player) -> boolean [static]

    private VanishCompat() {}

    private static boolean init() {
        if (isVanishedM != null) return true;
        if (absent) return false;
        try {
            Class<?> util = Class.forName("redstonedubstep.mods.vanishmod.VanishUtil");
            isVanishedM = util.getMethod("isVanished", Player.class);
            return true;
        } catch (Throwable t) {
            absent = true;
            return false;
        }
    }

    public static boolean isVanished(Player player) {
        if (player == null || !init()) return false;
        try {
            Object v = isVanishedM.invoke(null, player);
            return v instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
