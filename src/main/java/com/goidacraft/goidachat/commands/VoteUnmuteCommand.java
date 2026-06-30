package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.GoidaChat;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.vote.VoteMuteManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** /voteunmute &lt;игрок&gt; — голосование за снятие мута через GoidaVote. */
public class VoteUnmuteCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("voteunmute")
                .requires(src -> CommandUtil.has(src, "goidachat.votemute", 0))
                .then(Commands.argument("player", StringArgumentType.word()).suggests(CommandUtil.NAMES)
                        .executes(VoteUnmuteCommand::vote)));
    }

    private static int vote(CommandContext<CommandSourceStack> ctx) {
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
        UUID targetId = PlayerSessionCache.getUuid(targetName);
        if (targetId == null) { CommandUtil.msg(source, "&cИгрок не в сети."); return 0; }

        if (PunishmentStorage.getMute(targetId) == null) {
            CommandUtil.msg(source, "&cИгрок &e" + targetName + " &cне заглушён.");
            return 0;
        }
        if (manager.hasUnmuteCooldown(initiator.getUUID())) {
            CommandUtil.msg(source, "&cКулдаун команды. Повтор через: &f"
                    + formatCd(manager.getUnmuteCooldownRemaining(initiator.getUUID())));
            return 0;
        }
        if (manager.isTargetInActiveVote(targetId)) {
            CommandUtil.msg(source, "&cДля этого игрока уже идёт голосование.");
            return 0;
        }

        manager.startUnmuteVote(initiator, targetId, targetName);
        CommandUtil.msg(source, "&aГолосование за снятие мута запущено! Длительность: &f"
                + VoteMuteManager.VOTE_DURATION_SECONDS + "с&a. Порог: 66%.");
        return 1;
    }

    private static String formatCd(long ms) {
        long s = ms / 1000, h = s / 3600, m = (s % 3600) / 60;
        return h > 0 ? h + "ч " + m + "м" : m + "м " + (s % 60) + "с";
    }
}
