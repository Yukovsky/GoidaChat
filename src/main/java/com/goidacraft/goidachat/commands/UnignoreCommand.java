package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.IgnoreStorage;
import com.goidacraft.goidachat.data.PlayerHistory;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.util.ColorUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class UnignoreCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unignore")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String targetName = StringArgumentType.getString(ctx, "player");

                            UUID targetId = PlayerSessionCache.getUuid(targetName);
                            if (targetId == null) targetId = PlayerHistory.getUuidByName(targetName);
                            if (targetId == null) {
                                player.sendSystemMessage(ColorUtil.parse("&cИгрок не найден."));
                                return 0;
                            }

                            IgnoreStorage.removeIgnore(player.getUUID(), targetId);
                            player.sendSystemMessage(ColorUtil.parse(
                                    "&aИгрок &e" + targetName + " &aудалён из игнор-листа."));
                            return 1;
                        })));
    }
}
