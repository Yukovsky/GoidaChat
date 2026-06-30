package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.chat.ChatRouter;
import com.goidacraft.goidachat.util.ColorUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class AdminChatCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ac")
                .requires(src -> CommandUtil.has(src, "goidachat.adminchat", 2))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            String message = ColorUtil.sanitize(StringArgumentType.getString(ctx, "message"));
                            if (message.isEmpty()) return 0;
                            ChatRouter.handleAdmin(sender, message, sender.server.getPlayerList().getPlayers());
                            return 1;
                        })));
    }
}
