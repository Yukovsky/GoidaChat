package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.BanAttemptEntry;
import com.goidacraft.goidachat.data.BanAttemptStorage;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BanLogCommand {

    private static final int PER_PAGE = 10;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd.MM HH:mm:ss");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("banlog")
                .requires(src -> CommandUtil.has(src, "goidachat.banlog", 3))
                .executes(ctx -> show(ctx, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> show(ctx, IntegerArgumentType.getInteger(ctx, "page")))));
    }

    private static int show(CommandContext<CommandSourceStack> ctx, int page) {
        CommandSourceStack src = ctx.getSource();
        List<BanAttemptEntry> all = BanAttemptStorage.getAll();
        if (all.isEmpty()) { CommandUtil.msg(src, "&7Лог попыток входа пуст."); return 1; }

        int totalPages = (int) Math.ceil((double) all.size() / PER_PAGE);
        if (page < 1 || page > totalPages) {
            CommandUtil.msg(src, "&cСтраница " + page + " не существует. Всего: &e" + totalPages);
            return 0;
        }

        int from = (page - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, all.size());

        CommandUtil.msg(src, "&8&m         &r &c&lЛог попыток входа &7(стр. &e" + page
                + "&7/&e" + totalPages + "&7) &8&m         ");

        for (int i = from; i < to; i++) {
            BanAttemptEntry e = all.get(i);
            String time = FMT.format(new Date(e.timestamp));
            src.sendSystemMessage(ColorUtil.parse(
                    "&8[&7" + time + "&8] &e" + e.evaderName + " &8— &f" + buildDetail(e)));
        }

        src.sendSystemMessage(buildNavBar(page, totalPages, all.size()));
        return 1;
    }

    private static MutableComponent buildNavBar(int page, int totalPages, int total) {
        MutableComponent prev = page > 1
                ? Text.hover(Text.runCmd(ColorUtil.parse("&6◀ Назад"), "/banlog " + (page - 1)),
                        ColorUtil.parse("&eСтраница " + (page - 1)))
                : ColorUtil.parse("&8◀ Назад");
        MutableComponent next = page < totalPages
                ? Text.hover(Text.runCmd(ColorUtil.parse("&6Вперёд ▶"), "/banlog " + (page + 1)),
                        ColorUtil.parse("&eСтраница " + (page + 1)))
                : ColorUtil.parse("&8Вперёд ▶");

        return Component.empty()
                .append(prev)
                .append(ColorUtil.parse("  &7" + page + "/" + totalPages + " (&e" + total + " записей&7)  "))
                .append(next);
    }

    private static String buildDetail(BanAttemptEntry e) {
        return switch (e.via) {
            case "UUID" -> "&cUUID &7(прямой заход забаненного аккаунта)";
            case "IP"   -> "&cIP: &f" + nvl(e.ip) + " &7— обход бана &e" + e.bannedPlayer;
            case "HWID" -> "&cHWID: &f" + shortHwid(e.hwid) + " &7— обход бана &e" + e.bannedPlayer;
            default     -> "&7via " + e.via + " — обход бана &e" + e.bannedPlayer;
        };
    }

    private static String nvl(String s) { return s != null ? s : "?"; }

    private static String shortHwid(String hwid) {
        if (hwid == null) return "?";
        return hwid.length() > 12 ? hwid.substring(0, 12) + "…" : hwid;
    }
}
