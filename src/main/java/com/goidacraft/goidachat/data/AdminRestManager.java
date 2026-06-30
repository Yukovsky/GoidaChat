package com.goidacraft.goidachat.data;

import com.goidacraft.goidachat.util.LuckPermsUtil;
import net.minecraft.server.level.ServerPlayer;

public final class AdminRestManager {

    public enum Mode { NONE, REST, FULL_REST }

    private static volatile Mode current = Mode.NONE;

    private AdminRestManager() {}

    public static Mode getMode()       { return current; }
    public static boolean isActive()   { return current != Mode.NONE; }
    public static boolean isFullRest() { return current == Mode.FULL_REST; }

    public static void setMode(Mode mode) { current = mode; }

    /** Считается ли игрок администрацией (принимает личные сообщения как «админ»). */
    public static boolean isAdmin(ServerPlayer player) {
        return LuckPermsUtil.hasPermission(player, "goidachat.adminchat", 2);
    }
}
