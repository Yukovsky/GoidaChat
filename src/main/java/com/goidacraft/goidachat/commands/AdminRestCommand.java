package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.AdminRestManager;
import com.goidacraft.goidachat.data.AdminRestManager.Mode;
import com.goidacraft.goidachat.util.ColorUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdminRestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adminrest")
                .requires(src -> CommandUtil.has(src, "goidachat.adminrest", 3))
                .executes(ctx -> toggle(ctx, Mode.REST))
                .then(Commands.literal("full").executes(ctx -> toggle(ctx, Mode.FULL_REST))));
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx, Mode requested) {
        if (AdminRestManager.getMode() == requested) {
            AdminRestManager.setMode(Mode.NONE);
            broadcast(ctx, "&a[Администрация] &fОтдых завершён. Администрация снова на связи!");
        } else {
            AdminRestManager.setMode(requested);
            if (requested == Mode.FULL_REST) {
                broadcast(ctx, "&6[Администрация] &cПолный режим отдыха. "
                        + "Глобальный чат и личные сообщения администрации временно недоступны.");
            } else {
                broadcast(ctx, "&6[Администрация] &eРежим отдыха. "
                        + "Личные сообщения администрации временно недоступны.");
            }
        }
        return 1;
    }

    private static void broadcast(CommandContext<CommandSourceStack> ctx, String legacy) {
        Component msg = ColorUtil.parse(legacy);
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
        }
    }
}
