package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

/** Общие помощники для Brigadier-команд: проверка прав через LuckPerms + OP-fallback, подсказки. */
final class CommandUtil {

    private CommandUtil() {}

    /** Подсказка ников онлайн-игроков. */
    static final SuggestionProvider<CommandSourceStack> NAMES =
            (ctx, b) -> SharedSuggestionProvider.suggest(PlayerSessionCache.getAllNames(), b);

    /** Проверка права: игрок → LuckPerms с OP-fallback; консоль/команд-блок → уровень OP. */
    static boolean has(CommandSourceStack src, String node, int opLevel) {
        if (src.getEntity() instanceof ServerPlayer sp) {
            return LuckPermsUtil.hasPermission(sp, node, opLevel);
        }
        return src.hasPermission(opLevel);
    }

    static void msg(CommandSourceStack src, String legacy) {
        src.sendSystemMessage(ColorUtil.parse(legacy));
    }
}
