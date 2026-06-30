package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.*;
import com.goidacraft.goidachat.util.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /ban &lt;игрок&gt; &lt;срок&gt; [-s] &lt;причина&gt; — банит по UUID, а также по IP и HWID
 * (если включены и известны) под одним banId. -s делает бан тихим.
 */
public class BanCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ban")
                .requires(src -> CommandUtil.has(src, "goidachat.ban", 3))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        List.of("1h", "1d", "7d", "30d", "permanent"), b))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(BanCommand::ban)))));
    }

    private static int ban(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String targetName  = StringArgumentType.getString(ctx, "player");
        String durationStr = StringArgumentType.getString(ctx, "duration");

        long expiresAt;
        try { expiresAt = TimeUtil.parseDuration(durationStr); }
        catch (IllegalArgumentException e) { CommandUtil.msg(source, "&c" + e.getMessage()); return 0; }

        String[] words = StringArgumentType.getString(ctx, "reason").trim().split("\\s+");
        boolean silent = words.length > 0 && words[0].equals("-s");
        int start = silent ? 1 : 0;
        String reason = start < words.length
                ? ColorUtil.sanitize(String.join(" ", Arrays.copyOfRange(words, start, words.length)))
                : "Нарушение правил";
        if (reason.isBlank()) reason = "Нарушение правил";

        UUID targetId = PlayerSessionCache.getUuid(targetName);
        ServerPlayer online = targetId != null
                ? source.getServer().getPlayerList().getPlayer(targetId) : null;
        if (targetId == null) targetId = PlayerHistory.getUuidByName(targetName);
        if (targetId == null) {
            CommandUtil.msg(source, "&cИгрок не найден. Он должен хотя бы раз заходить на сервер.");
            return 0;
        }

        String ip   = resolveIp(online, targetId);
        String hwid = resolveHwid(online, targetId);

        String banId    = UUID.randomUUID().toString();
        String bannedBy = source.getTextName();
        List<BanEntry> entries = new ArrayList<>();
        List<String>   applied = new ArrayList<>();

        entries.add(new BanEntry(banId, BanType.UUID, targetId.toString(), targetName, reason, expiresAt, bannedBy));
        applied.add("UUID");

        if (PluginConfig.enableIpBan()) {
            if (ip != null) {
                entries.add(new BanEntry(banId, BanType.IP, ip, targetName, reason, expiresAt, bannedBy));
                applied.add("IP");
            } else {
                CommandUtil.msg(source, "&7[!] IP неизвестен — бан по IP пропущен.");
            }
        }
        if (PluginConfig.enableHwidBan()) {
            if (hwid != null) {
                entries.add(new BanEntry(banId, BanType.HWID, hwid, targetName, reason, expiresAt, bannedBy));
                applied.add("HWID");
            } else {
                CommandUtil.msg(source, "&7[!] HWID неизвестен (мод HWID не установлен или игрок ещё не заходил) — пропущен.");
            }
        }

        PunishmentStorage.addBans(entries);
        if (online != null) online.connection.disconnect(BanScreens.build(entries.get(0)));

        String remaining = TimeUtil.formatRemaining(expiresAt);
        String banMsg = "&cИгрок &e" + targetName + " &cзабанен (" + String.join("+", applied)
                + "). Причина: &f" + reason + " &c| Срок: &f" + remaining;

        if (silent) {
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                if (LuckPermsUtil.hasPermission(p, "goidachat.ban", 3)) {
                    p.sendSystemMessage(ColorUtil.parse("&8[Тихо] " + banMsg));
                }
            }
            CommandUtil.msg(source, "&8[Тихо] " + banMsg);
        } else {
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(ColorUtil.parse(banMsg));
            }
        }
        return 1;
    }

    private static String resolveIp(ServerPlayer online, UUID targetId) {
        if (online != null) return IpUtil.getIp(online);
        PlayerRecord rec = PlayerHistory.get(targetId);
        return rec != null ? rec.lastIp : null;
    }

    private static String resolveHwid(ServerPlayer online, UUID targetId) {
        if (!HwidLookup.isAvailable()) return null;
        PlayerRecord rec = PlayerHistory.get(targetId);
        if (rec != null && rec.lastHwid != null) return rec.lastHwid;
        if (online != null) return HwidLookup.resolveHwid(targetId);
        return null;
    }
}
