package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.GoidaChat;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.vote.VoteMuteManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * /votemute &lt;игрок&gt; [30m|1h|2h|3h] — голосование за мут через GoidaVote.
 * Регистрируется только когда GoidaVote установлен (см. CommandRegistrar).
 */
public class VoteMuteCommand {

    private static final long M30 = 30L * 60 * 1000, H1 = 60L * 60 * 1000,
                              H2 = 120L * 60 * 1000, H3 = 180L * 60 * 1000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("votemute")
                .requires(src -> CommandUtil.has(src, "goidachat.votemute", 0))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(CommandUtil.NAMES)
                        .executes(ctx -> vote(ctx, "1h"))
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        List.of("30m", "1h", "2h", "3h"), b))
                                .executes(ctx -> vote(ctx, StringArgumentType.getString(ctx, "duration"))))));
    }

    private static int vote(CommandContext<CommandSourceStack> ctx, String durationArg) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer initiator;
        try { initiator = source.getPlayerOrException(); }
        catch (Exception e) { CommandUtil.msg(source, "&cТолько игроки могут использовать эту команду."); return 0; }

        VoteMuteManager manager = GoidaChat.voteManager();
        if (manager == null || !manager.isAvailable()) {
            CommandUtil.msg(source, "&cГолосование недоступно: GoidaVote не установлен.");
            return 0;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        if (targetName.equalsIgnoreCase(initiator.getGameProfile().getName())) {
            CommandUtil.msg(source, "&cНельзя голосовать за мут самого себя.");
            return 0;
        }

        UUID targetId = PlayerSessionCache.getUuid(targetName);
        if (targetId == null) { CommandUtil.msg(source, "&cИгрок не в сети."); return 0; }

        if (manager.hasMuteCooldown(initiator.getUUID())) {
            CommandUtil.msg(source, "&cКулдаун команды. Повтор через: &f"
                    + formatCd(manager.getMuteCooldownRemaining(initiator.getUUID())));
            return 0;
        }
        if (manager.isTargetInActiveVote(targetId)) {
            CommandUtil.msg(source, "&cДля этого игрока уже идёт голосование.");
            return 0;
        }

        long durationMs = parseDuration(durationArg.toLowerCase());
        if (durationMs <= 0) {
            CommandUtil.msg(source, "&cНеверная длительность. Доступно: 30m, 1h, 2h, 3h");
            return 0;
        }

        manager.startMuteVote(initiator, targetId, targetName, durationMs, durationLabel(durationArg.toLowerCase()));
        CommandUtil.msg(source, "&aГолосование запущено! Длительность: &f"
                + VoteMuteManager.VOTE_DURATION_SECONDS + "с&a. Порог: 66%.");
        return 1;
    }

    private static long parseDuration(String s) {
        return switch (s) {
            case "30m" -> M30; case "1h" -> H1; case "2h" -> H2; case "3h" -> H3;
            default -> -1;
        };
    }

    private static String durationLabel(String s) {
        return switch (s) {
            case "30m" -> "30 минут"; case "1h" -> "1 час";
            case "2h" -> "2 часа"; case "3h" -> "3 часа";
            default -> s;
        };
    }

    private static String formatCd(long ms) {
        long s = ms / 1000, h = s / 3600, m = (s % 3600) / 60;
        return h > 0 ? h + "ч " + m + "м" : m + "м " + (s % 60) + "с";
    }
}
