package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.chat.AntiSpamManager;
import com.goidacraft.goidachat.chat.ChatFormatter;
import com.goidacraft.goidachat.data.AdminRestManager;
import com.goidacraft.goidachat.data.IgnoreStorage;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.logging.ChatLogger;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class MsgCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var msg = dispatcher.register(Commands.literal("msg")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = ctx.getSource().getServer().getPlayerList()
                                            .getPlayerByName(StringArgumentType.getString(ctx, "player"));
                                    return sendPm(sender, target, StringArgumentType.getString(ctx, "message"));
                                }))));

        dispatcher.register(Commands.literal("tell").redirect(msg));
        dispatcher.register(Commands.literal("w").redirect(msg));
    }

    /** Отправляет ЛС с проверками (мут/спам/отдых/игнор), соцшпионом и логированием. */
    static int sendPm(ServerPlayer sender, ServerPlayer target, String rawMessage) {
        if (target == null) {
            sender.sendSystemMessage(ColorUtil.parse("&cИгрок не найден или не в сети."));
            return 0;
        }
        if (target.getUUID().equals(sender.getUUID())) {
            sender.sendSystemMessage(ColorUtil.parse("&cНельзя написать себе."));
            return 0;
        }
        if (PunishmentStorage.getMute(sender.getUUID()) != null) {
            sender.sendSystemMessage(ColorUtil.parse("&cВы заглушены."));
            return 0;
        }
        if (AntiSpamManager.isSpamming(sender)) {
            sender.sendSystemMessage(ColorUtil.parse("&cВы пишете слишком быстро."));
            return 0;
        }

        String message = ColorUtil.sanitize(rawMessage);
        if (message.isEmpty()) return 0;

        if (AdminRestManager.isActive() && AdminRestManager.isAdmin(target)) {
            sender.sendSystemMessage(ColorUtil.parse(
                    "&cАдминистрация сейчас не принимает заявки и не решает проблемы. "
                    + "Дайте администрации отдохнуть!"));
            return 0;
        }

        if (IgnoreStorage.isIgnoredPm(target.getUUID(), sender.getUUID())) {
            sender.sendSystemMessage(ColorUtil.parse("&cЭтот игрок вас игнорирует."));
            return 0;
        }

        sender.sendSystemMessage(ChatFormatter.formatPmOut(sender, target, message));
        target.sendSystemMessage(ChatFormatter.formatPmIn(sender, target, message));

        PlayerSessionCache.setLastMsgPartner(sender.getUUID(), target.getUUID());

        MutableComponent spyMsg = ChatFormatter.formatPmSpy(sender, target, message);
        for (ServerPlayer spy : sender.server.getPlayerList().getPlayers()) {
            if (!spy.getUUID().equals(sender.getUUID())
                    && !spy.getUUID().equals(target.getUUID())
                    && PlayerSessionCache.hasSocialSpy(spy.getUUID())
                    && LuckPermsUtil.hasPermission(spy, "goidachat.socialspy", 2)) {
                spy.sendSystemMessage(spyMsg);
            }
        }

        ChatLogger.log("PM", sender.getGameProfile().getName() + " -> "
                + target.getGameProfile().getName(), message);
        return 1;
    }
}
