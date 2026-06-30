package com.goidacraft.goidachat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * NeoForge server config (config/goidachat-server.toml). Covers the full feature set:
 * chat formatting, moderation/ban-evasion, anti-spam, auto-moderation (ads/caps/profanity),
 * escalation and logging. Read these values through {@link PluginConfig} from logic code.
 */
public final class GoidaChatConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    private GoidaChatConfig() {}

    // ---- chat ----
    public static final ModConfigSpec.IntValue LOCAL_RADIUS;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_PREFIX;
    public static final ModConfigSpec.ConfigValue<String> LOCAL_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> GLOBAL_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> ADMIN_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> MENTION_SOUND;
    public static final ModConfigSpec.ConfigValue<String> NO_ONE_HEARD;

    // ---- moderation ----
    public static final ModConfigSpec.BooleanValue ENABLE_IP_BAN;
    public static final ModConfigSpec.BooleanValue ENABLE_HWID_BAN;
    public static final ModConfigSpec.ConfigValue<String> HWID_FOLDER;
    public static final ModConfigSpec.BooleanValue BAN_EVADE_NOTIFY;
    public static final ModConfigSpec.BooleanValue AUTO_BAN_EVADER;
    public static final ModConfigSpec.ConfigValue<String> APPEAL_CONTACT;

    // ---- anti-spam ----
    public static final ModConfigSpec.IntValue SPAM_WINDOW_MS;
    public static final ModConfigSpec.IntValue SPAM_MAX_MESSAGES;
    public static final ModConfigSpec.IntValue SPAM_DUPLICATE_COUNT;
    public static final ModConfigSpec.BooleanValue CHAR_REPEAT_ENABLED;
    public static final ModConfigSpec.IntValue CHAR_REPEAT_MAX;

    // ---- auto-moderation: advertising ----
    public static final ModConfigSpec.BooleanValue ADVERTISING_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADVERTISING_WHITELIST;

    // ---- auto-moderation: caps ----
    public static final ModConfigSpec.BooleanValue CAPS_ENABLED;
    public static final ModConfigSpec.DoubleValue CAPS_THRESHOLD;
    public static final ModConfigSpec.IntValue CAPS_MIN_LENGTH;
    public static final ModConfigSpec.ConfigValue<String> CAPS_ACTION;

    // ---- auto-moderation: profanity ----
    public static final ModConfigSpec.BooleanValue PROFANITY_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> PROFANITY_ACTION;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PROFANITY_WORDS;

    // ---- escalation ----
    public static final ModConfigSpec.IntValue ESCALATION_RESET_HOURS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ESCALATION_MUTE_DURATIONS;
    public static final ModConfigSpec.BooleanValue ESCALATION_NOTIFY_STAFF;

    // ---- logging ----
    public static final ModConfigSpec.IntValue LOG_RETENTION_DAYS;

    static {
        BUILDER.push("chat");
        LOCAL_RADIUS  = BUILDER.comment("Radius in blocks for local chat")
                .defineInRange("localRadius", 50, 1, 10000);
        GLOBAL_PREFIX = BUILDER.comment("Prefix that routes a message to global chat")
                .define("globalPrefix", "!");
        LOCAL_FORMAT  = BUILDER.comment("Local format. Placeholders: %prefix% %player% %suffix% %message%")
                .define("localFormat", "&8[&aL&8] &r%prefix%%player%%suffix% &e>&r %message%");
        GLOBAL_FORMAT = BUILDER.comment("Global chat format")
                .define("globalFormat", "&8[&aG&8] &r%prefix%%player%%suffix% &e>&r %message%");
        ADMIN_FORMAT  = BUILDER.comment("Admin chat format")
                .define("adminFormat", "&8[&cA&8] &r%prefix%%player%%suffix% &e>&r %message%");
        MENTION_SOUND = BUILDER.comment("Sound played on mention (namespace:key)")
                .define("mentionSound", "minecraft:entity.experience_orb.pickup");
        NO_ONE_HEARD  = BUILDER.comment("Shown when no one is within local chat radius")
                .define("noOneHeard", "&7Вас никто не слышит.");
        BUILDER.pop();

        BUILDER.push("moderation");
        ENABLE_IP_BAN    = BUILDER.comment("Enable banning and ban-checking by IP address")
                .define("enableIpBan", true);
        ENABLE_HWID_BAN  = BUILDER.comment("Enable banning by hardware ID (requires the HWID mod)")
                .define("enableHwidBan", true);
        HWID_FOLDER      = BUILDER.comment("Path to the HWID mod data folder, relative to the server root")
                .define("hwidFolder", "config/hwid");
        BAN_EVADE_NOTIFY = BUILDER.comment("Notify online staff when an IP/HWID-banned player tries to join")
                .define("banEvadeNotify", true);
        AUTO_BAN_EVADER  = BUILDER.comment("Automatically ban a fresh account that joins from a banned IP/HWID")
                .define("autoBanEvader", true);
        APPEAL_CONTACT   = BUILDER.comment("Appeal contact shown on the ban screen")
                .define("appealContact", "discord.gg/example");
        BUILDER.pop();

        BUILDER.push("antispam");
        SPAM_WINDOW_MS       = BUILDER.comment("Spam detection window in milliseconds")
                .defineInRange("windowMs", 2000, 100, 60000);
        SPAM_MAX_MESSAGES    = BUILDER.comment("Max messages allowed in the spam window")
                .defineInRange("maxMessages", 3, 1, 100);
        SPAM_DUPLICATE_COUNT = BUILDER.comment("How many identical messages in a row are allowed (0 = off)")
                .defineInRange("duplicateCheckCount", 3, 0, 100);
        CHAR_REPEAT_ENABLED  = BUILDER.comment("Block excessive repetition of a single character")
                .define("charRepeatEnabled", true);
        CHAR_REPEAT_MAX      = BUILDER.comment("Max consecutive identical characters (0 = off)")
                .defineInRange("charRepeatMax", 4, 0, 100);
        BUILDER.pop();

        BUILDER.push("automod");

        BUILDER.push("advertising");
        ADVERTISING_ENABLED   = BUILDER.comment("Block links/domains outside the whitelist")
                .define("enabled", true);
        ADVERTISING_WHITELIST = BUILDER.comment("Domains/paths allowed to be posted in chat")
                .defineList("whitelist",
                        List.of("goidacraft.ru", "discord.gg/goidacraft"),
                        () -> "", o -> o instanceof String);
        BUILDER.pop();

        BUILDER.push("caps");
        CAPS_ENABLED   = BUILDER.comment("Check the ratio of uppercase letters")
                .define("enabled", true);
        CAPS_THRESHOLD = BUILDER.comment("Uppercase ratio threshold (0.0-1.0)")
                .defineInRange("threshold", 0.70, 0.0, 1.0);
        CAPS_MIN_LENGTH = BUILDER.comment("Minimum message length to check")
                .defineInRange("minLength", 8, 1, 256);
        CAPS_ACTION    = BUILDER.comment("LOWERCASE = convert and pass; BLOCK = block the message")
                .define("action", "LOWERCASE");
        BUILDER.pop();

        BUILDER.push("profanity");
        PROFANITY_ENABLED = BUILDER.comment("Filter blacklisted words")
                .define("enabled", false);
        PROFANITY_ACTION  = BUILDER.comment("REPLACE = mask with asterisks; BLOCK = block the message")
                .define("action", "REPLACE");
        PROFANITY_WORDS   = BUILDER.comment("Blacklisted words")
                .defineListAllowEmpty("words", List.of(), () -> "", o -> o instanceof String);
        BUILDER.pop();

        BUILDER.pop(); // automod

        BUILDER.push("escalation");
        ESCALATION_RESET_HOURS    = BUILDER.comment("Hours without violations before the counter resets")
                .defineInRange("violationResetHours", 24, 1, 8760);
        ESCALATION_MUTE_DURATIONS = BUILDER.comment("Mute durations by escalation step (1st = warning, 2nd = [0], ...)")
                .defineList("muteDurations",
                        List.of("5m", "30m", "1d", "7d"),
                        () -> "5m", o -> o instanceof String);
        ESCALATION_NOTIFY_STAFF   = BUILDER.comment("Notify staff (goidachat.mute) about auto-mutes")
                .define("notifyStaff", true);
        BUILDER.pop();

        BUILDER.push("logging");
        LOG_RETENTION_DAYS = BUILDER.comment("How many days to keep chat logs")
                .defineInRange("retentionDays", 7, 1, 365);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
