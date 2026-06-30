package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.chat.EscalationManager;
import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.ViolationEntry;
import com.goidacraft.goidachat.data.ViolationStorage;
import com.goidacraft.goidachat.data.ViolationType;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class ViolationsCommand {

    private static final int PAGE_SIZE = 8;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm").withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("violations")
                .requires(src -> CommandUtil.has(src, "goidachat.violations", 3))
                .executes(ctx -> { sendHelp(ctx.getSource()); return 1; })
                .then(Commands.literal("list")
                        .executes(ctx -> listAll(ctx.getSource(), 1))
                        .then(Commands.argument("arg", StringArgumentType.word())
                                .suggests(CommandUtil.NAMES)
                                .executes(ctx -> listArg(ctx.getSource(), StringArgumentType.getString(ctx, "arg"), 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> listArg(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "arg"),
                                                IntegerArgumentType.getInteger(ctx, "page"))))))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(CommandUtil.NAMES)
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> add(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                StringArgumentType.getString(ctx, "reason"))))))
                .then(Commands.literal("edit")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> edit(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "reason"))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(CommandUtil.NAMES)
                                .executes(ctx -> clear(ctx.getSource(), StringArgumentType.getString(ctx, "player"))))));
    }

    // ---------------------------------------------------------------- list

    private static int listArg(CommandSourceStack src, String arg, int page) {
        try {
            return listAll(src, Integer.parseInt(arg));
        } catch (NumberFormatException e) {
            return listPlayer(src, arg, page);
        }
    }

    private static int listAll(CommandSourceStack src, int page) {
        return render(src, ViolationStorage.getAll(), null, page);
    }

    private static int listPlayer(CommandSourceStack src, String player, int page) {
        return render(src, ViolationStorage.getByPlayerName(player), player, page);
    }

    private static int render(CommandSourceStack src, List<ViolationEntry> entries, String filter, int page) {
        if (entries.isEmpty()) {
            CommandUtil.msg(src, filter != null
                    ? "&7Нарушений у игрока &e" + filter + " &7не найдено."
                    : "&7Список нарушений пуст.");
            return 1;
        }
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        page = Math.max(1, Math.min(page, totalPages));
        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, entries.size());

        String title = filter != null ? "Нарушения: &e" + filter : "Все нарушения";
        src.sendSystemMessage(ColorUtil.parse("&8════ &c&l" + title + " &8(&e" + page + "&8/&e"
                + totalPages + "&8) — всего: &e" + entries.size() + " &8════"));
        for (int i = from; i < to; i++) src.sendSystemMessage(buildRow(entries.get(i), i - from + 1));

        String base = "/violations list" + (filter != null ? " " + filter : "");
        src.sendSystemMessage(buildNav(base, page, totalPages));
        return 1;
    }

    private static Component buildRow(ViolationEntry e, int index) {
        String date   = DATE_FMT.format(Instant.ofEpochMilli(e.timestamp));
        String reason = e.reason.length() > 28 ? e.reason.substring(0, 28) + "…" : e.reason;
        return Component.empty()
                .append(ColorUtil.parse("&8" + index + ". "))
                .append(Text.hover(Text.runCmd(ColorUtil.parse("&7[" + e.id + "] "), "/violations info " + e.id),
                        ColorUtil.parse("&7Подробности")))
                .append(ColorUtil.parse("&e" + e.playerName + " &8│ " + typeCode(e.type) + e.type.displayName
                        + " &8│ &f" + reason + " &8│ &3" + date));
    }

    private static Component buildNav(String base, int page, int totalPages) {
        var prev = page > 1
                ? Text.runCmd(ColorUtil.parse("&b[◀ Назад] "), base + " " + (page - 1))
                : ColorUtil.parse("&8[◀ Назад] ");
        var next = page < totalPages
                ? Text.runCmd(ColorUtil.parse(" &b[Вперёд ▶]"), base + " " + (page + 1))
                : ColorUtil.parse(" &8[Вперёд ▶]");
        return Component.empty().append(prev)
                .append(ColorUtil.parse("&8Стр. " + page + "/" + totalPages)).append(next);
    }

    // ---------------------------------------------------------------- info

    private static int info(CommandSourceStack src, String id) {
        ViolationEntry e = ViolationStorage.get(id);
        if (e == null) { CommandUtil.msg(src, "&cНарушение &e" + id + " &cне найдено."); return 0; }

        String date = DATE_FMT.format(Instant.ofEpochMilli(e.timestamp));
        int level = EscalationManager.getCount(e.playerUuid);

        src.sendSystemMessage(ColorUtil.parse("&8━━━ Нарушение: &e&l" + e.id + " &8━━━"));
        src.sendSystemMessage(ColorUtil.parse(" &7Игрок: &e" + e.playerName));
        src.sendSystemMessage(ColorUtil.parse(" &7Тип: " + typeCode(e.type) + e.type.displayName));
        src.sendSystemMessage(ColorUtil.parse(" &7Эскалация: " + escalationCode(level) + "уровень " + level
                + " &8→ &6" + nextPunishment(level)));
        src.sendSystemMessage(ColorUtil.parse(" &7Причина: &f" + e.reason));
        src.sendSystemMessage(ColorUtil.parse(" &7Детали: &3"
                + (e.details.length() > 60 ? e.details.substring(0, 60) + "…" : e.details)));
        src.sendSystemMessage(ColorUtil.parse(" &7Дата: &f" + date));
        src.sendSystemMessage(Component.empty()
                .append(Text.hover(Text.suggest(ColorUtil.parse("&6[✎ Изменить] "), "/violations edit " + e.id + " "),
                        ColorUtil.parse("&7Изменить причину")))
                .append(Text.hover(Text.runCmd(ColorUtil.parse("&c[✗ Удалить]"), "/violations delete " + e.id),
                        ColorUtil.parse("&7Удалить нарушение"))));
        src.sendSystemMessage(ColorUtil.parse("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return 1;
    }

    // ---------------------------------------------------------------- add/edit/delete/clear

    private static int add(CommandSourceStack src, String targetName, String reason) {
        UUID targetUuid;
        ServerPlayer online = src.getServer().getPlayerList().getPlayerByName(targetName);
        if (online != null) {
            targetUuid = online.getUUID();
            targetName = online.getGameProfile().getName();
        } else {
            List<ViolationEntry> existing = ViolationStorage.getByPlayerName(targetName);
            if (existing.isEmpty()) {
                CommandUtil.msg(src, "&cИгрок &e" + targetName + " &cне в сети и не найден в записях.");
                return 0;
            }
            targetUuid = existing.get(0).playerUuid;
            targetName = existing.get(0).playerName;
        }
        ViolationEntry entry = ViolationStorage.add(targetUuid, targetName, ViolationType.MANUAL, "вручную", reason);
        CommandUtil.msg(src, "&aНарушение добавлено. ID: &e" + entry.id);
        return 1;
    }

    private static int edit(CommandSourceStack src, String id, String reason) {
        if (!ViolationStorage.update(id, reason)) {
            CommandUtil.msg(src, "&cНарушение &e" + id + " &cне найдено.");
            return 0;
        }
        CommandUtil.msg(src, "&aПричина нарушения &e" + id + " &aобновлена.");
        return 1;
    }

    private static int delete(CommandSourceStack src, String id) {
        if (!ViolationStorage.delete(id)) {
            CommandUtil.msg(src, "&cНарушение &e" + id + " &cне найдено.");
            return 0;
        }
        CommandUtil.msg(src, "&aНарушение &e" + id + " &aудалено.");
        return 1;
    }

    private static int clear(CommandSourceStack src, String player) {
        List<ViolationEntry> existing = ViolationStorage.getByPlayerName(player);
        if (existing.isEmpty()) {
            CommandUtil.msg(src, "&cНарушений у игрока &e" + player + " &cне найдено.");
            return 0;
        }
        UUID target = existing.get(0).playerUuid;
        int count = ViolationStorage.clearByPlayer(target);
        EscalationManager.reset(target);
        CommandUtil.msg(src, "&aУдалено &e" + count + " &aнарушений игрока &e" + player
                + "&a, счётчик эскалации сброшен.");
        return 1;
    }

    // ---------------------------------------------------------------- helpers

    private static void sendHelp(CommandSourceStack src) {
        CommandUtil.msg(src, "&8&m══════════&r &6/violations &8&m══════════");
        CommandUtil.msg(src, "&e/violations list &8[страница] &7— все нарушения");
        CommandUtil.msg(src, "&e/violations list &b<игрок> &8[страница] &7— нарушения игрока");
        CommandUtil.msg(src, "&e/violations info &b<id> &7— подробности");
        CommandUtil.msg(src, "&e/violations add &b<игрок> <причина> &7— добавить вручную");
        CommandUtil.msg(src, "&e/violations edit &b<id> <причина> &7— изменить причину");
        CommandUtil.msg(src, "&e/violations delete &b<id> &7— удалить нарушение");
        CommandUtil.msg(src, "&e/violations clear &b<игрок> &7— очистить все нарушения игрока");
    }

    private static String typeCode(ViolationType type) {
        return switch (type) {
            case FLOOD       -> "&c";
            case DUPLICATE   -> "&6";
            case CHAR_REPEAT -> "&e";
            case ADVERTISING -> "&b";
            case CAPS        -> "&6";
            case PROFANITY   -> "&4";
            case MANUAL      -> "&d";
        };
    }

    private static String escalationCode(int count) {
        if (count == 0) return "&a";
        if (count == 1) return "&e";
        if (count <= 3) return "&6";
        return "&c";
    }

    private static String nextPunishment(int count) {
        List<String> durations = PluginConfig.escalationMuteDurations();
        if (durations.isEmpty()) return "нет";
        if (count == 0) return "предупреждение";
        int nextIdx = count - 1;
        if (nextIdx >= durations.size()) return "мут " + durations.get(durations.size() - 1);
        return "мут " + durations.get(nextIdx);
    }
}
