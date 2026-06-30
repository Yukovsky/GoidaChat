package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.IgnoreStorage;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.util.ColorUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class IgnoreCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ignore")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .executes(ctx -> ignore(ctx, true))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("pm", "all"), b))
                                .executes(ctx -> ignore(ctx,
                                        !StringArgumentType.getString(ctx, "type").equalsIgnoreCase("pm"))))));
    }

    private static int ignore(CommandContext<CommandSourceStack> ctx, boolean all) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(ctx, "player");

        UUID targetId = PlayerSessionCache.getUuid(targetName);
        if (targetId == null) {
            player.sendSystemMessage(ColorUtil.parse("&cИгрок не в сети."));
            return 0;
        }
        if (targetId.equals(player.getUUID())) {
            player.sendSystemMessage(ColorUtil.parse("&cНельзя игнорировать себя."));
            return 0;
        }

        IgnoreStorage.addIgnore(player.getUUID(), targetId, all);
        player.sendSystemMessage(ColorUtil.parse(
                "&aИгрок &e" + targetName + " &aигнорируется (" + (all ? "все" : "ЛС") + ")."));
        return 1;
    }
}
