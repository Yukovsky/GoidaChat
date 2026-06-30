package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.MuteEntry;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import com.goidacraft.goidachat.util.TimeUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** /mute &lt;игрок&gt; &lt;срок&gt; [-s] [-v] &lt;причина&gt; — флаги перед причиной: -s тихо, -v + голос. */
public class MuteCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mute")
                .requires(src -> CommandUtil.has(src, "goidachat.mute", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        List.of("10m", "1h", "1d", "permanent"), b))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(MuteCommand::mute)))));

        dispatcher.register(Commands.literal("unmute")
                .requires(src -> CommandUtil.has(src, "goidachat.mute", 2))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .executes(MuteCommand::unmute)));
    }

    private static int mute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String targetName  = StringArgumentType.getString(ctx, "player");
        String durationStr = StringArgumentType.getString(ctx, "duration");

        UUID targetId = PlayerSessionCache.getUuid(targetName);
        if (targetId == null) { CommandUtil.msg(source, "&cИгрок не в сети."); return 0; }

        long expiresAt;
        try { expiresAt = TimeUtil.parseDuration(durationStr); }
        catch (IllegalArgumentException e) { CommandUtil.msg(source, "&c" + e.getMessage()); return 0; }

        Flags flags = parseFlags(StringArgumentType.getString(ctx, "reason"));

        MuteEntry entry = new MuteEntry(targetId, targetName, flags.reason, expiresAt,
                source.getTextName(), flags.voice);
        PunishmentStorage.addMute(entry);

        String remaining = TimeUtil.formatRemaining(expiresAt);
        String voiceTag  = flags.voice ? " &c(+голос)" : "";
        String muteMsg = "&cИгрок &e" + targetName + " &cзаглушён" + voiceTag + ". Причина: &f"
                + flags.reason + " &c| На: &f" + remaining;

        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target != null) target.sendSystemMessage(ColorUtil.parse(
                "&cВы заглушены" + (flags.voice ? " в чате и голосовом чате" : "")
                + ". Причина: &f" + flags.reason + " &c| На: &f" + remaining));

        broadcast(source, muteMsg, flags.silent);
        return 1;
    }

    private static int unmute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String targetName = StringArgumentType.getString(ctx, "player");

        UUID targetId = PlayerSessionCache.getUuid(targetName);
        if (targetId == null) { CommandUtil.msg(source, "&cИгрок не в сети."); return 0; }

        PunishmentStorage.removeMute(targetId);
        CommandUtil.msg(source, "&aИгрок &e" + targetName + " &aразмучен.");

        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target != null) target.sendSystemMessage(ColorUtil.parse("&aВаш мут снят."));
        return 1;
    }

    private static void broadcast(CommandSourceStack source, String message, boolean silent) {
        if (silent) {
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                if (LuckPermsUtil.hasPermission(p, "goidachat.mute", 2)) {
                    p.sendSystemMessage(ColorUtil.parse("&7[Тихо] " + message));
                }
            }
            CommandUtil.msg(source, "&7[Тихо] " + message);
        } else {
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(ColorUtil.parse(message));
            }
        }
    }

    private static Flags parseFlags(String raw) {
        Flags f = new Flags();
        String[] words = raw.trim().split("\\s+");
        int idx = 0;
        while (idx < words.length) {
            if (words[idx].equals("-s"))      { f.silent = true; idx++; }
            else if (words[idx].equals("-v")) { f.voice  = true; idx++; }
            else break;
        }
        f.reason = idx < words.length
                ? ColorUtil.sanitize(String.join(" ", Arrays.copyOfRange(words, idx, words.length)))
                : "Нарушение правил";
        if (f.reason.isBlank()) f.reason = "Нарушение правил";
        return f;
    }

    private static final class Flags {
        boolean silent;
        boolean voice;
        String  reason;
    }
}
