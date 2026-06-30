package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public class KickCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kick")
                .requires(src -> CommandUtil.has(src, "goidachat.kick", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(KickCommand::kick))));
    }

    private static int kick(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String targetName = StringArgumentType.getString(ctx, "player");

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) { CommandUtil.msg(source, "&cИгрок не в сети."); return 0; }

        String[] words = StringArgumentType.getString(ctx, "reason").trim().split("\\s+");
        boolean silent = words.length > 0 && words[0].equals("-s");
        int start = silent ? 1 : 0;
        String reason = start < words.length
                ? ColorUtil.sanitize(String.join(" ", Arrays.copyOfRange(words, start, words.length)))
                : "Нарушение правил";
        if (reason.isBlank()) reason = "Нарушение правил";

        target.connection.disconnect(ColorUtil.parse("&cВас кикнули.\n&eПричина: &f" + reason));

        String msg = "&eИгрок &c" + target.getGameProfile().getName() + " &eвыкинут. Причина: &f" + reason;
        if (silent) {
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                if (LuckPermsUtil.hasPermission(p, "goidachat.kick", 2)) {
                    p.sendSystemMessage(ColorUtil.parse("&7[Тихо] " + msg));
                }
            }
            CommandUtil.msg(source, "&7[Тихо] " + msg);
        } else {
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(ColorUtil.parse(msg));
            }
        }
        return 1;
    }
}
