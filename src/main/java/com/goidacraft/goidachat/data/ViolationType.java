package com.goidacraft.goidachat.data;

public enum ViolationType {
    FLOOD("Флуд"),
    DUPLICATE("Дубликат"),
    CHAR_REPEAT("Символьный спам"),
    ADVERTISING("Реклама"),
    CAPS("Капслок"),
    PROFANITY("Мат"),
    MANUAL("Вручную");

    public final String displayName;

    ViolationType(String displayName) { this.displayName = displayName; }
}
