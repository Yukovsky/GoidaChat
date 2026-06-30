package com.goidacraft.goidachat.commands;

import com.goidacraft.goidachat.config.PluginConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * /automod [reload|status]. На NeoForge значения конфига читаются «вживую» (ModConfigSpec),
 * а правки файла config/goidachat-server.toml подхватываются автоматически при перезагрузке
 * конфига. Команда показывает текущее состояние автомодерации.
 */
public class AutoModCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("automod")
                .requires(src -> CommandUtil.has(src, "goidachat.automod", 3))
                .then(Commands.literal("reload").executes(AutoModCommand::status))
                .then(Commands.literal("status").executes(AutoModCommand::status))
                .executes(AutoModCommand::status));
    }

    private static int status(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        CommandUtil.msg(src, "&8&m═══════&r &6Автомодерация &8&m═══════");
        CommandUtil.msg(src, "&7Реклама: " + onOff(PluginConfig.advertisingEnabled()));
        CommandUtil.msg(src, "&7Капслок: " + onOff(PluginConfig.capsEnabled())
                + " &8(порог " + (int) (PluginConfig.capsThreshold() * 100) + "%, "
                + PluginConfig.capsAction() + ")");
        CommandUtil.msg(src, "&7Мат: " + onOff(PluginConfig.profanityEnabled())
                + " &8(" + PluginConfig.profanityAction() + ", слов: "
                + PluginConfig.profanityWords().size() + ")");
        CommandUtil.msg(src, "&7Антиспам: &aвкл &8(" + PluginConfig.spamMaxMessages()
                + " сообщ. / " + PluginConfig.spamWindowMs() + "мс)");
        CommandUtil.msg(src, "&7Эскалация: &8шаги " + PluginConfig.escalationMuteDurations());
        CommandUtil.msg(src, "&7Значения читаются вживую из &fconfig/goidachat-server.toml&7.");
        return 1;
    }

    private static String onOff(boolean b) { return b ? "&aвкл" : "&cвыкл"; }
}
