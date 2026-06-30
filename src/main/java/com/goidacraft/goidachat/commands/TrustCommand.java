package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.BanEntry;
import com.goidacraft.goidachat.data.PlayerHistory;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.data.TrustedAccounts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.UUID;

public class TrustCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trustaccount")
                .requires(src -> CommandUtil.has(src, "goidachat.ban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .executes(TrustCommand::trust)));
    }

    private static int trust(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String targetName = StringArgumentType.getString(ctx, "player");

        UUID uuid = PlayerSessionCache.getUuid(targetName);
        if (uuid == null) uuid = PlayerHistory.getUuidByName(targetName);
        if (uuid == null) {
            CommandUtil.msg(src, "&cИгрок не найден. Он должен хотя бы раз заходить на сервер.");
            return 0;
        }

        if (TrustedAccounts.contains(uuid)) {
            CommandUtil.msg(src, "&e" + targetName + " &7уже в списке доверенных.");
            return 0;
        }

        TrustedAccounts.add(uuid);

        BanEntry existing = PunishmentStorage.getBanByUuid(uuid);
        int removed = existing != null ? PunishmentStorage.removeGroup(existing.banId) : 0;

        String msg = "&aАккаунт &e" + targetName + " &aдобавлен в доверенные — IP и HWID баны обходятся.";
        if (removed > 0) msg += " &7(снят авто-бан, удалено записей: " + removed + ")";
        CommandUtil.msg(src, msg);
        return 1;
    }
}
