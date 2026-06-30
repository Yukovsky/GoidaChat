package com.goidacraft.goidachat.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin typed accessor over {@link GoidaChatConfig} (NeoForge ModConfigSpec). Method names mirror
 * the original Bukkit config API so the ported chat/moderation logic reads from a single source.
 * Values are read live, so editing config/goidachat-server.toml takes effect on the next read.
 */
public final class PluginConfig {

    private PluginConfig() {}

    // ---- chat ----
    public static int    localRadius()  { return GoidaChatConfig.LOCAL_RADIUS.get(); }
    public static String globalPrefix() { return GoidaChatConfig.GLOBAL_PREFIX.get(); }
    public static String localFormat()  { return GoidaChatConfig.LOCAL_FORMAT.get(); }
    public static String globalFormat() { return GoidaChatConfig.GLOBAL_FORMAT.get(); }
    public static String adminFormat()  { return GoidaChatConfig.ADMIN_FORMAT.get(); }
    public static String mentionSound() { return GoidaChatConfig.MENTION_SOUND.get(); }
    public static String noOneHeard()   { return GoidaChatConfig.NO_ONE_HEARD.get(); }

    // ---- moderation ----
    public static boolean enableIpBan()    { return GoidaChatConfig.ENABLE_IP_BAN.get(); }
    public static boolean enableHwidBan()  { return GoidaChatConfig.ENABLE_HWID_BAN.get(); }
    public static String  hwidFolder()     { return GoidaChatConfig.HWID_FOLDER.get(); }
    public static boolean banEvadeNotify() { return GoidaChatConfig.BAN_EVADE_NOTIFY.get(); }
    public static boolean autoBanEvader()  { return GoidaChatConfig.AUTO_BAN_EVADER.get(); }
    public static String  appealContact()  { return GoidaChatConfig.APPEAL_CONTACT.get(); }

    // ---- anti-spam ----
    public static int     spamWindowMs()      { return GoidaChatConfig.SPAM_WINDOW_MS.get(); }
    public static int     spamMaxMessages()   { return GoidaChatConfig.SPAM_MAX_MESSAGES.get(); }
    public static int     spamDuplicateCount(){ return GoidaChatConfig.SPAM_DUPLICATE_COUNT.get(); }
    public static boolean charRepeatEnabled() { return GoidaChatConfig.CHAR_REPEAT_ENABLED.get(); }
    public static int     charRepeatMax()     { return GoidaChatConfig.CHAR_REPEAT_MAX.get(); }

    // ---- auto-moderation ----
    public static boolean advertisingEnabled()      { return GoidaChatConfig.ADVERTISING_ENABLED.get(); }
    public static List<String> advertisingWhitelist() { return copy(GoidaChatConfig.ADVERTISING_WHITELIST.get()); }

    public static boolean capsEnabled()   { return GoidaChatConfig.CAPS_ENABLED.get(); }
    public static double  capsThreshold() { return GoidaChatConfig.CAPS_THRESHOLD.get(); }
    public static int     capsMinLength() { return GoidaChatConfig.CAPS_MIN_LENGTH.get(); }
    public static String  capsAction()    { return GoidaChatConfig.CAPS_ACTION.get(); }

    public static boolean profanityEnabled()    { return GoidaChatConfig.PROFANITY_ENABLED.get(); }
    public static String  profanityAction()     { return GoidaChatConfig.PROFANITY_ACTION.get(); }
    public static List<String> profanityWords() { return copy(GoidaChatConfig.PROFANITY_WORDS.get()); }

    // ---- escalation ----
    public static int     escalationResetHours()       { return GoidaChatConfig.ESCALATION_RESET_HOURS.get(); }
    public static List<String> escalationMuteDurations() { return copy(GoidaChatConfig.ESCALATION_MUTE_DURATIONS.get()); }
    public static boolean escalationNotifyStaff()      { return GoidaChatConfig.ESCALATION_NOTIFY_STAFF.get(); }

    // ---- logging ----
    public static int logRetentionDays() { return GoidaChatConfig.LOG_RETENTION_DAYS.get(); }

    private static List<String> copy(List<? extends String> src) {
        return src == null ? List.of() : new ArrayList<>(src);
    }
}
