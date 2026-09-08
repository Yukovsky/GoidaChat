package com.goidacraft.goidachat.chat;

import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import com.goidacraft.goidachat.util.VanishCompat;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

/**
 * Сборка компонентов чата из строк-форматов конфига. Текст сообщения вставляется как литерал
 * (или как уже подсвеченный компонент упоминаний) — без разбора цветовых кодов, чтобы игроки
 * не могли инжектить форматирование. Ник кликабелен (предлагает /msg ник).
 */
public final class ChatFormatter {

    private ChatFormatter() {}

    public static MutableComponent formatLocal(ServerPlayer sender, MutableComponent message) {
        return buildChat(PluginConfig.localFormat(), sender, message, true);
    }

    public static MutableComponent formatGlobal(ServerPlayer sender, MutableComponent message) {
        return buildChat(PluginConfig.globalFormat(), sender, message, true);
    }

    public static MutableComponent formatAdmin(ServerPlayer sender, MutableComponent message) {
        return buildChat(PluginConfig.adminFormat(), sender, message, false);
    }

    public static MutableComponent formatPmOut(ServerPlayer sender, ServerPlayer target, String message) {
        return ColorUtil.parse("&7[&eЯ &7→ &e" + VanishCompat.displayName(target) + "&7] &f")
                .append(Component.literal(message));
    }

    public static MutableComponent formatPmIn(ServerPlayer sender, ServerPlayer target, String message) {
        return ColorUtil.parse("&7[&e" + VanishCompat.displayName(sender) + " &7→ &eЯ&7] &f")
                .append(Component.literal(message));
    }

    public static MutableComponent formatPmSpy(ServerPlayer sender, ServerPlayer target, String message) {
        return ColorUtil.parse("&8[Spy] &7" + sender.getGameProfile().getName()
                + " → " + target.getGameProfile().getName() + ": &f")
                .append(Component.literal(message));
    }

    private static MutableComponent buildChat(String format, ServerPlayer sender, MutableComponent message,
                                               boolean maskVanished) {
        String prefix = LuckPermsUtil.getPrefix(sender);
        String suffix = LuckPermsUtil.getSuffix(sender);
        String name   = sender.getGameProfile().getName();
        boolean vanished = maskVanished && VanishCompat.isVanished(sender);

        String[] parts = format.split("%player%", 2);

        MutableComponent result = Component.empty();
        result.append(ColorUtil.parse(parts[0]
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix)));

        String nickText;
        Style nickStyle;
        if (vanished) {
            // Ник вейнш-игрока не выбивается и в обычном чате: показываем той же длины заглушку
            // без /msg-подсказки, ни в тексте, ни в наведении реальный ник не всплывает.
            nickText = VanishCompat.maskedNickname();
            nickStyle = Style.EMPTY.withObfuscated(true)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            ColorUtil.parse("&cЭтому игроку нельзя написать")));
        } else {
            nickText = name;
            nickStyle = Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + name + " "))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            ColorUtil.parse("&7Написать &e" + name)));
        }
        result.append(Component.literal(nickText).withStyle(nickStyle));

        if (parts.length > 1) {
            String[] msgParts = parts[1].split("%message%", 2);
            String beforeMsg = msgParts[0]
                    .replace("%prefix%", prefix)
                    .replace("%suffix%", suffix);
            
            result.append(ColorUtil.parse(beforeMsg));
            
            if (msgParts.length > 1) {
                Style messageStyle = ColorUtil.getLastStyle(beforeMsg);
                result.append(message.copy().withStyle(messageStyle));
                
                if (!msgParts[1].isEmpty()) {
                    result.append(ColorUtil.parse(msgParts[1]));
                }
            }
        }
        return result;
    }
}
