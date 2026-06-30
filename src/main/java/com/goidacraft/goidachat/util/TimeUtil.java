package com.goidacraft.goidachat.util;

public class TimeUtil {

    public static long parseDuration(String input) {
        if (input == null || input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perm")) {
            return Long.MAX_VALUE;
        }
        try {
            char unit = Character.toLowerCase(input.charAt(input.length() - 1));
            long value = Long.parseLong(input.substring(0, input.length() - 1));
            return switch (unit) {
                case 's' -> System.currentTimeMillis() + value * 1000L;
                case 'm' -> System.currentTimeMillis() + value * 60_000L;
                case 'h' -> System.currentTimeMillis() + value * 3_600_000L;
                case 'd' -> System.currentTimeMillis() + value * 86_400_000L;
                default -> throw new IllegalArgumentException("Unknown unit: " + unit);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid duration format. Use: 10s, 5m, 2h, 7d, permanent");
        }
    }

    public static String formatRemaining(long expiresAt) {
        if (expiresAt == Long.MAX_VALUE) return "навсегда";
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) return "истекло";
        long seconds = remaining / 1000;
        if (seconds < 60) return seconds + " сек.";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " мин.";
        long hours = minutes / 60;
        if (hours < 24) return hours + " ч. " + (minutes % 60) + " мин.";
        long days = hours / 24;
        return days + " д. " + (hours % 24) + " ч.";
    }

    public static boolean isExpired(long expiresAt) {
        return expiresAt != Long.MAX_VALUE && System.currentTimeMillis() >= expiresAt;
    }
}
