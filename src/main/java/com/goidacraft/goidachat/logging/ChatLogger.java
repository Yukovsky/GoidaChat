package com.goidacraft.goidachat.logging;

import com.goidacraft.goidachat.config.GoidaChatConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ChatLogger {

    private static final Logger LOGGER = LogManager.getLogger("GoidaChat");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static Path logDir;

    public static void init(Path gameDir) {
        logDir = gameDir.resolve("logs").resolve("goidachat");
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create goidachat log dir", e);
        }
        // cleanOldLogs() is called separately from ServerStartingEvent (after config loads)
    }

    public static void cleanOldLogs() {
        if (logDir == null) return;
        int retentionDays = GoidaChatConfig.LOG_RETENTION_DAYS.get();
        cleanOldLogs(retentionDays);
    }

    public static void log(String type, String player, String message) {
        String time = LocalDateTime.now().format(TIME_FMT);
        String line = "[" + time + "] [" + type + "] " + player + ": " + message;
        CompletableFuture.runAsync(() -> appendLine(line));
    }

    private static void appendLine(String line) {
        Path file = logDir.resolve(LocalDate.now().format(DATE_FMT) + ".log");
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            LOGGER.error("Failed to write chat log", e);
        }
    }

    private static void cleanOldLogs(int retentionDays) {
        LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
        try (Stream<Path> files = Files.list(logDir)) {
            files.filter(p -> p.toString().endsWith(".log"))
                    .filter(p -> {
                        try {
                            String name = p.getFileName().toString().replace(".log", "");
                            LocalDate date = LocalDate.parse(name, DATE_FMT);
                            return date.isBefore(cutoff);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException e) {
                            LOGGER.warn("Failed to delete old log: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to clean old logs", e);
        }
    }
}
