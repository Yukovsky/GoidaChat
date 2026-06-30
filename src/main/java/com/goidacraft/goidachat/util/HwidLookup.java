package com.goidacraft.goidachat.util;

import com.goidacraft.goidachat.config.GoidaChatConfig;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Интеграция с модом HWID (Anti-Alts) https://modrinth.com/mod/hwid
 *
 * Мод HWID сам по себе НЕ банит — он лишь складывает в {@code <server>/config/hwid/}
 * по файлу на каждый аппаратный хеш; имя файла = HWID-хеш, содержимое = список
 * UUID аккаунтов, заходивших с этого «железа». Публичного API у мода нет, поэтому
 * GoidaChat читает эти файлы напрямую: ищет файл, в котором есть UUID игрока,
 * и имя этого файла является его HWID. Сам бан по HWID реализован в GoidaChat.
 */
public final class HwidLookup {

    private static final Logger LOGGER = LogManager.getLogger("GoidaChat");

    /** Корень сервера (game dir). Папка hwid резолвится лениво — после загрузки конфига. */
    private static Path gameDir;

    private HwidLookup() {}

    public static void init(Path serverGameDir) {
        gameDir = serverGameDir;
    }

    /** Папка данных мода HWID. Резолвится лениво, т.к. путь берётся из конфига. */
    private static Path hwidDir() {
        if (gameDir == null) return null;
        return gameDir.resolve(GoidaChatConfig.HWID_FOLDER.get());
    }

    /** Доступна ли интеграция: мод HWID загружен или его папка данных существует. */
    public static boolean isAvailable() {
        if (ModList.get() != null && ModList.get().isLoaded("hwid")) return true;
        Path dir = hwidDir();
        return dir != null && Files.isDirectory(dir);
    }

    /**
     * Находит HWID-хеш игрока: имя файла в папке hwid, содержащего его UUID.
     * Возвращает null, если HWID ещё не записан модом или интеграция выключена.
     * Делает дисковое чтение — вызывать вне основного потока сервера.
     */
    public static String resolveHwid(UUID uuid) {
        Path dir = hwidDir();
        if (dir == null || !Files.isDirectory(dir)) return null;

        String target = normalize(uuid.toString());
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (!Files.isRegularFile(f)) continue;
                try {
                    List<String> lines = Files.readAllLines(f);
                    for (String line : lines) {
                        if (normalize(line.trim()).equals(target)) {
                            return f.getFileName().toString();
                        }
                    }
                } catch (IOException ignored) {
                    // файл мог быть удалён/перезаписан модом HWID — пропускаем
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Не удалось прочитать папку HWID: {}", dir, e);
        }
        return null;
    }

    /** UUID в файлах HWID может быть с дефисами или без — сравниваем без них, в нижнем регистре. */
    private static String normalize(String s) {
        return s.replace("-", "").toLowerCase();
    }
}
