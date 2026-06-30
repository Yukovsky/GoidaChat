package com.goidacraft.goidachat.chat;

import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.ViolationType;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ContentFilter {

    private static boolean isBypass(ServerPlayer p) {
        return LuckPermsUtil.hasPermission(p, "goidachat.antispam.bypass", 2);
    }

    // Покрывает http(s)://, www. и bare-домены с популярными TLD
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(?i)(https?://\\S+|www\\.\\S+|\\S+\\.(?:com|ru|net|org|gg|io|me|xyz|su|biz|club|site|online|pro|cc)\\b)"
    );

    private ContentFilter() {}

    // ---------------------------------------------------------------- Result

    public static final class Result {
        public final ViolationType type;
        public final String        details;
        /** null → заблокировать; non-null → пропустить, заменив текст этим значением. */
        public final String        modified;

        Result(ViolationType type, String details, String modified) {
            this.type     = type;
            this.details  = details;
            this.modified = modified;
        }

        public boolean isBlock() { return modified == null; }
    }

    // ---------------------------------------------------------------- Advertising

    public static Result checkAdvertising(ServerPlayer player, String message) {
        if (!PluginConfig.advertisingEnabled() || isBypass(player)) return null;

        Matcher m = URL_PATTERN.matcher(message);
        if (!m.find()) return null;

        String found = m.group();
        for (String allowed : PluginConfig.advertisingWhitelist()) {
            if (found.toLowerCase().contains(allowed.toLowerCase())) return null;
        }
        return new Result(ViolationType.ADVERTISING, found, null);
    }

    // ---------------------------------------------------------------- Caps

    public static Result checkCaps(ServerPlayer player, String message) {
        if (!PluginConfig.capsEnabled() || isBypass(player)) return null;
        if (message.length() < PluginConfig.capsMinLength()) return null;

        int total = 0, upper = 0;
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                total++;
                if (Character.isUpperCase(c)) upper++;
            }
        }
        if (total == 0 || (double) upper / total < PluginConfig.capsThreshold()) return null;

        double ratio    = (double) upper / total;
        String details  = String.format("%.0f%% заглавных", ratio * 100);
        boolean doLower = PluginConfig.capsAction().equalsIgnoreCase("LOWERCASE");
        return new Result(ViolationType.CAPS, details, doLower ? message.toLowerCase() : null);
    }

    // ---------------------------------------------------------------- Profanity

    public static Result checkProfanity(ServerPlayer player, String message) {
        if (!PluginConfig.profanityEnabled() || isBypass(player)) return null;

        List<String> words = PluginConfig.profanityWords();
        if (words.isEmpty()) return null;

        String lower = message.toLowerCase();
        String found = null;
        for (String w : words) {
            if (lower.contains(w.toLowerCase())) { found = w; break; }
        }
        if (found == null) return null;

        if (PluginConfig.profanityAction().equalsIgnoreCase("REPLACE")) {
            String modified = message;
            for (String w : words) {
                modified = modified.replaceAll("(?i)" + Pattern.quote(w), "*".repeat(w.length()));
            }
            return new Result(ViolationType.PROFANITY, found, modified);
        }
        return new Result(ViolationType.PROFANITY, found, null);
    }
}
