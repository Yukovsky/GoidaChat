package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import com.goidacraft.goidachat.util.VanishCompat;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Общие помощники для Brigadier-команд: проверка прав через LuckPerms + OP-fallback, подсказки. */
final class CommandUtil {

    private CommandUtil() {}

    /** Подсказка ников онлайн-игроков с учётом Vanish (скрывает невидимых админов от обычных игроков). */
    static final SuggestionProvider<CommandSourceStack> NAMES = (ctx, builder) -> {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer sender = src.getEntity() instanceof ServerPlayer sp ? sp : null;
        if (src.getServer() == null) {
            return SharedSuggestionProvider.suggest(PlayerSessionCache.getAllNames(), builder);
        }
        List<String> visible = new ArrayList<>();
        for (ServerPlayer player : src.getServer().getPlayerList().getPlayers()) {
            if (sender != null && VanishCompat.isVanished(player, sender)) {
                continue;
            }
            visible.add(player.getGameProfile().getName());
        }
        return SharedSuggestionProvider.suggest(visible, builder);
    };

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
