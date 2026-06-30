package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.util.ColorUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class SocialSpyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("socialspy")
                .requires(src -> CommandUtil.has(src, "goidachat.socialspy", 2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    PlayerSessionCache.toggleSocialSpy(player.getUUID());
                    boolean enabled = PlayerSessionCache.hasSocialSpy(player.getUUID());
                    player.sendSystemMessage(ColorUtil.parse(
                            enabled ? "&aSocialSpy включён." : "&cSocialSpy выключен."));
                    return 1;
                }));
    }
}
