package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.VanishCompat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ReplyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var reply = dispatcher.register(Commands.literal("r")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            UUID partnerId = PlayerSessionCache.getLastMsgPartner(sender.getUUID());
                            if (partnerId == null) {
                                sender.sendSystemMessage(ColorUtil.parse("&cНет собеседника для ответа."));
                                return 0;
                            }
                            ServerPlayer target = sender.server.getPlayerList().getPlayer(partnerId);
                            if (target == null || VanishCompat.isVanished(target, sender)) {
                                sender.sendSystemMessage(ColorUtil.parse("&cИгрок не найден или не в сети."));
                                return 0;
                            }
                            return MsgCommand.sendPm(sender, target, StringArgumentType.getString(ctx, "message"));
                        })));

        dispatcher.register(Commands.literal("reply").redirect(reply));
    }
}
