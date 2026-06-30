package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.data.BanEntry;
import com.goidacraft.goidachat.data.BanType;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.Text;
import com.goidacraft.goidachat.util.TimeUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public class BanListCommand {

    private static final int PAGE_SIZE = 8;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("banlist")
                .requires(src -> CommandUtil.has(src, "goidachat.ban", 3))
                .executes(ctx -> list(ctx, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> list(ctx, IntegerArgumentType.getInteger(ctx, "page"))))
                .then(Commands.literal("info")
                        .then(Commands.argument("banId", StringArgumentType.word())
                                .executes(ctx -> {
                                    showBanInfo(ctx.getSource(), StringArgumentType.getString(ctx, "banId"));
                                    return 1;
                                }))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx, int page) {
        CommandSourceStack src = ctx.getSource();
        List<BanEntry> bans = PunishmentStorage.getAllUuidBans();
        if (bans.isEmpty()) { CommandUtil.msg(src, "&7Список банов пуст."); return 1; }

        int totalPages = (int) Math.ceil(bans.size() / (double) PAGE_SIZE);
        page = Math.max(1, Math.min(page, totalPages));
        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, bans.size());

        src.sendSystemMessage(ColorUtil.parse("&8════ &c&lБаны плагина &8(&e" + page + "&8/&e"
                + totalPages + "&8) — всего: &e" + bans.size() + " &8════"));
        for (int i = from; i < to; i++) src.sendSystemMessage(buildBanRow(bans.get(i), i - from + 1));
        src.sendSystemMessage(buildNavigation(page, totalPages));
        return 1;
    }

    private static void showBanInfo(CommandSourceStack src, String banId) {
        List<BanEntry> entries = PunishmentStorage.getBansByBanId(banId);
        if (entries.isEmpty()) { CommandUtil.msg(src, "&cБан не найден или уже истёк."); return; }

        BanEntry primary = entries.stream().filter(e -> e.type == BanType.UUID).findFirst().orElse(entries.get(0));
        String ip   = entries.stream().filter(e -> e.type == BanType.IP).map(e -> e.target).findFirst().orElse(null);
        String hwid = entries.stream().filter(e -> e.type == BanType.HWID).map(e -> e.target).findFirst().orElse(null);
        String expires = primary.expiresAt == Long.MAX_VALUE ? "навсегда" : TimeUtil.formatRemaining(primary.expiresAt);

        src.sendSystemMessage(ColorUtil.parse("&8━━━ Бан: &e&l" + primary.playerName + " &8━━━"));
        src.sendSystemMessage(ColorUtil.parse(" &7Причина: &f" + primary.reason));
        src.sendSystemMessage(ColorUtil.parse(" &7Срок: &6" + expires));
        src.sendSystemMessage(ColorUtil.parse(" &7Кем: &f" + primary.bannedBy));
        src.sendSystemMessage(ColorUtil.parse(" &7UUID: &a" + (primary.type == BanType.UUID ? primary.target : "—")));

        src.sendSystemMessage(Component.empty().append(ColorUtil.parse(" &7IP: ")).append(ip != null
                ? Text.hover(Text.copy(ColorUtil.parse("&b" + ip), ip), ColorUtil.parse("&7Нажми, чтобы скопировать"))
                : ColorUtil.parse("&8не забанен по IP")));
        src.sendSystemMessage(Component.empty().append(ColorUtil.parse(" &7HWID: ")).append(hwid != null
                ? Text.hover(Text.copy(ColorUtil.parse("&d" + hwid), hwid), ColorUtil.parse("&7Нажми, чтобы скопировать"))
                : ColorUtil.parse("&8не забанен по HWID")));
        src.sendSystemMessage(ColorUtil.parse("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private static MutableComponent buildBanRow(BanEntry e, int index) {
        List<BanType> types = PunishmentStorage.getTypesForBanId(e.banId);
        String expires = e.expiresAt == Long.MAX_VALUE ? "∞" : TimeUtil.formatRemaining(e.expiresAt);

        MutableComponent name = Text.hover(
                Text.runCmd(ColorUtil.parse("&e&l" + e.playerName), "/banlist info " + e.banId),
                ColorUtil.parse("&7Нажми для подробностей\n&bIP / HWID / UUID"));

        return Component.empty()
                .append(ColorUtil.parse("&8" + index + ". "))
                .append(name)
                .append(ColorUtil.parse(" &8│ &f" + e.reason + " &8│ &6" + expires + " &8[" + typesToString(types) + "&8]"));
    }

    private static MutableComponent buildNavigation(int page, int totalPages) {
        MutableComponent prev = page > 1
                ? Text.hover(Text.runCmd(ColorUtil.parse("&b[◀ Назад]"), "/banlist " + (page - 1)),
                        ColorUtil.parse("&7Страница " + (page - 1)))
                : ColorUtil.parse("&8[◀ Назад]");
        MutableComponent next = page < totalPages
                ? Text.hover(Text.runCmd(ColorUtil.parse("&b[Вперёд ▶]"), "/banlist " + (page + 1)),
                        ColorUtil.parse("&7Страница " + (page + 1)))
                : ColorUtil.parse("&8[Вперёд ▶]");

        return Component.empty().append(prev)
                .append(ColorUtil.parse(" &8Стр. " + page + "/" + totalPages + " "))
                .append(next);
    }

    private static String typesToString(List<BanType> types) {
        StringBuilder sb = new StringBuilder();
        for (BanType t : types) {
            if (sb.length() > 0) sb.append("&7+");
            switch (t) {
                case UUID -> sb.append("&aU");
                case IP   -> sb.append("&bI");
                case HWID -> sb.append("&dH");
            }
        }
        return sb.toString();
    }
}
