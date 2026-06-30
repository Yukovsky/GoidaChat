package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.PlayerHistory;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.data.TrustedAccounts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.UUID;

public class UntrustCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("untrustaccount")
                .requires(src -> CommandUtil.has(src, "goidachat.ban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .executes(UntrustCommand::untrust)));
    }

    private static int untrust(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String targetName = StringArgumentType.getString(ctx, "player");

        UUID uuid = PlayerSessionCache.getUuid(targetName);
        if (uuid == null) uuid = PlayerHistory.getUuidByName(targetName);
        if (uuid == null) {
            CommandUtil.msg(src, "&cИгрок не найден. Он должен хотя бы раз заходить на сервер.");
            return 0;
        }

        if (!TrustedAccounts.remove(uuid)) {
            CommandUtil.msg(src, "&e" + targetName + " &7не находится в списке доверенных.");
            return 0;
        }

        CommandUtil.msg(src, "&aАккаунт &e" + targetName + " &aудалён из списка доверенных.");
        return 1;
    }
}
