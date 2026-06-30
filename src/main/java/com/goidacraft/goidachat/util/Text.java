package com.goidacraft.goidachat.util;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * Small helpers for building interactive vanilla chat components (click / hover / copy),
 * used by the paginated admin list commands (banlist, banlog, violations).
 */
public final class Text {

    private Text() {}

    /** Parse legacy &-coded text into a component. */
    public static MutableComponent of(String legacy) { return ColorUtil.parse(legacy); }

    /** Attach a RUN_COMMAND click action. */
    public static MutableComponent runCmd(MutableComponent c, String command) {
        return c.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    /** Attach a SUGGEST_COMMAND click action (puts the text into the chat box). */
    public static MutableComponent suggest(MutableComponent c, String command) {
        return c.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
    }

    /** Attach a COPY_TO_CLIPBOARD click action. */
    public static MutableComponent copy(MutableComponent c, String value) {
        return c.withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value)));
    }

    /** Attach a SHOW_TEXT hover tooltip. */
    public static MutableComponent hover(MutableComponent c, Component tooltip) {
        return c.withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)));
    }
}
