package com.goidacraft.goidachat.util;

import com.goidacraft.goidachat.config.GoidaChatConfig;
import com.goidacraft.goidachat.data.BanEntry;
import net.minecraft.network.chat.MutableComponent;

/** Сборка экрана блокировки, показываемого при кике/входе забаненного игрока. */
public final class BanScreens {

    private BanScreens() {}

    public static MutableComponent build(BanEntry ban) {
        String appeal = GoidaChatConfig.APPEAL_CONTACT.get();
        String remaining = ban.expiresAt == Long.MAX_VALUE
                ? "навсегда"
                : "осталось " + TimeUtil.formatRemaining(ban.expiresAt);

        String typeLine = switch (ban.type) {
            case IP -> "&7Блокировка по IP-адресу.\n";
            case HWID -> "&7Блокировка по оборудованию (HWID).\n";
            default -> "";
        };

        return ColorUtil.parse(
                "&c&lВы заблокированы на этом сервере\n\n"
                + "&eПричина: &f" + safe(ban.reason) + "\n"
                + "&eСрок: &f" + remaining + "\n"
                + typeLine
                + "\n&7Апелляция: &b" + appeal
        );
    }

    private static String safe(String s) {
        return s == null ? "не указана" : ColorUtil.sanitize(s);
    }
}
