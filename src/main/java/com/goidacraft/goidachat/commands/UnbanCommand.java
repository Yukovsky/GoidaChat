package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.BanEntry;
import com.goidacraft.goidachat.data.PlayerHistory;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.UUID;

/** /unban &lt;игрок|IP|HWID&gt; — снимает всю группу записей бана (тот же banId). */
public class UnbanCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var unban = dispatcher.register(Commands.literal("unban")
                .requires(src -> CommandUtil.has(src, "goidachat.ban", 3))
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(CommandUtil.NAMES)
                        .executes(UnbanCommand::unban)));
        dispatcher.register(Commands.literal("pardon").redirect(unban));
    }

    private static int unban(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String target = StringArgumentType.getString(ctx, "target");

        BanEntry found = findBan(target);
        if (found == null) {
            CommandUtil.msg(src, "&cАктивная блокировка не найдена: &e" + target);
            return 0;
        }

        int removed = PunishmentStorage.removeGroup(found.banId);
        CommandUtil.msg(src, "&aСнята блокировка игрока &e" + found.playerName
                + " &a(записей удалено: " + removed + ").");
        return 1;
    }

    private static BanEntry findBan(String target) {
        UUID uuid = PlayerSessionCache.getUuid(target);
        if (uuid == null) uuid = PlayerHistory.getUuidByName(target);
        if (uuid == null) uuid = tryParseUuid(target);
        if (uuid != null) {
            BanEntry b = PunishmentStorage.getBanByUuid(uuid);
            if (b != null) return b;
        }
        BanEntry byIp = PunishmentStorage.getBanByIp(target);
        if (byIp != null) return byIp;
        return PunishmentStorage.getBanByHwid(target);
    }

    private static UUID tryParseUuid(String s) {
        try { return UUID.fromString(s); }
        catch (IllegalArgumentException e) { return null; }
    }
}
