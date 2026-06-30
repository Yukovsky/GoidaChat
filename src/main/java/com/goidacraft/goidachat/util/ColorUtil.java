package com.goidacraft.goidachat.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public class ColorUtil {

    public static MutableComponent parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        MutableComponent root = Component.empty();
        StringBuilder current = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    if (!current.isEmpty()) {
                        root.append(Component.literal(current.toString()).withStyle(currentStyle));
                        current.setLength(0);
                    }
                    if (fmt == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (fmt.isFormat()) {
                        currentStyle = applyFormat(currentStyle, fmt);
                    } else {
                        currentStyle = Style.EMPTY.withColor(fmt);
                    }
                    i++;
                    continue;
                }
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            root.append(Component.literal(current.toString()).withStyle(currentStyle));
        }

        return root;
    }

    public static Style getLastStyle(String text) {
        if (text == null || text.isEmpty()) return Style.EMPTY;
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    if (fmt == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (fmt.isFormat()) {
                        currentStyle = applyFormat(currentStyle, fmt);
                    } else {
                        currentStyle = Style.EMPTY.withColor(fmt);
                    }
                    i++;
                }
            }
        }
        return currentStyle;
    }

    /**
     * Очищает текст, введённый игроком, перед показом другим игрокам:
     *  - убирает символ секции '§' (иначе клиент применит legacy-форматирование);
     *  - заменяет переносы строк на пробел (защита от подделки строк/логов);
     *  - удаляет управляющие символы.
     * Символ '&' остаётся как есть — он не интерпретируется в обычном тексте.
     */
    public static String sanitize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '§') continue;
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
                continue;
            }
            if (c < ' ') continue;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private static Style applyFormat(Style style, ChatFormatting fmt) {
        return switch (fmt) {
            case BOLD -> style.withBold(true);
            case ITALIC -> style.withItalic(true);
            case UNDERLINE -> style.withUnderlined(true);
            case STRIKETHROUGH -> style.withStrikethrough(true);
            case OBFUSCATED -> style.withObfuscated(true);
            default -> style;
        };
    }
}
