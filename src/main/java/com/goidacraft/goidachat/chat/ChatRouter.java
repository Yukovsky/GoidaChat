package com.goidacraft.goidachat.chat;

import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.AdminRestManager;
import com.goidacraft.goidachat.data.IgnoreStorage;
import com.goidacraft.goidachat.logging.ChatLogger;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public final class ChatRouter {

    private ChatRouter() {}

    public static void handleLocal(ServerPlayer sender, String rawMessage, Collection<ServerPlayer> allPlayers) {
        MentionProcessor.Result mention = MentionProcessor.process(rawMessage);
        MutableComponent formatted = ChatFormatter.formatLocal(sender, mention.component());

        int radius = PluginConfig.localRadius();
        long radiusSq = (long) radius * radius;

        List<ServerPlayer> recipients = new ArrayList<>();
        boolean heardBySomeoneElse = false;

        for (ServerPlayer p : allPlayers) {
            boolean isSender = p.getUUID().equals(sender.getUUID());
            if (isSender) { p.sendSystemMessage(formatted); continue; }
            if (!p.level().dimension().equals(sender.level().dimension())) continue;
            if (p.distanceToSqr(sender) > radiusSq) continue;
            if (IgnoreStorage.isIgnoredAll(p.getUUID(), sender.getUUID())) continue;
            p.sendSystemMessage(formatted);
            recipients.add(p);
            heardBySomeoneElse = true;
        }

        if (!heardBySomeoneElse) {
            sender.sendSystemMessage(ColorUtil.parse(PluginConfig.noOneHeard()));
        }

        MentionProcessor.playMentionSounds(mention, recipients);
        ChatLogger.log("LOCAL", sender.getGameProfile().getName(), rawMessage);
    }

    public static void handleGlobal(ServerPlayer sender, String rawMessage, Collection<ServerPlayer> allPlayers) {
        if (AdminRestManager.isFullRest() && !AdminRestManager.isAdmin(sender)) {
            sender.sendSystemMessage(ColorUtil.parse(
                    "&cГлобальный чат временно отключён. Администрация отдыхает!"));
            return;
        }

        MentionProcessor.Result mention = MentionProcessor.process(rawMessage);
        MutableComponent formatted = ChatFormatter.formatGlobal(sender, mention.component());

        List<ServerPlayer> recipients = new ArrayList<>();
        for (ServerPlayer p : allPlayers) {
            boolean isSender = p.getUUID().equals(sender.getUUID());
            if (!isSender && IgnoreStorage.isIgnoredAll(p.getUUID(), sender.getUUID())) continue;
            p.sendSystemMessage(formatted);
            if (!isSender) recipients.add(p);
        }

        MentionProcessor.playMentionSounds(mention, recipients);
        ChatLogger.log("GLOBAL", sender.getGameProfile().getName(), rawMessage);
    }

    public static void handleAdmin(ServerPlayer sender, String rawMessage, Collection<ServerPlayer> allPlayers) {
        MentionProcessor.Result mention = MentionProcessor.process(rawMessage);
        MutableComponent formatted = ChatFormatter.formatAdmin(sender, mention.component());

        List<ServerPlayer> recipients = new ArrayList<>();
        for (ServerPlayer p : allPlayers) {
            if (LuckPermsUtil.hasPermission(p, "goidachat.adminchat", 2)) {
                p.sendSystemMessage(formatted);
                recipients.add(p);
            }
        }

        MentionProcessor.playMentionSounds(mention, recipients);
        ChatLogger.log("ADMIN", sender.getGameProfile().getName(), rawMessage);
    }
}
