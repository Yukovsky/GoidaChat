package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.util.ColorUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class MentionSoundCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("togglemention")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    PlayerSessionCache.toggleMentionSound(player.getUUID());
                    boolean disabled = PlayerSessionCache.isMentionSoundDisabled(player.getUUID());
                    player.sendSystemMessage(ColorUtil.parse(
                            disabled ? "&cЗвук упоминания отключён." : "&aЗвук упоминания включён."));
                    return 1;
                })
        );
    }
}
