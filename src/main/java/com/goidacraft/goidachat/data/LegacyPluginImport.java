package com.goidacraft.goidachat.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Однократный перенос данных <b>плагина</b> GoidaChat в каталог конфигурации мода.
 *
 * <p>Плагин и мод используют байт-идентичный JSON и одинаковые имена файлов, различаясь только
 * базовым каталогом: плагин хранил всё в {@code <server>/plugins/GoidaChat/goidachat/}, а мод
 * читает из {@code <server>/config/goidachat/}. Поэтому миграция — это прямое копирование файлов,
 * без разбора и преобразования. Логи у обеих версий лежат в одном месте
 * ({@code <server>/logs/goidachat/}) и переноса не требуют.
 *
 * <p>Выполняется один раз: маркер {@code .migrated} в целевом каталоге предотвращает повторный
 * импорт (правки администратора после миграции никогда не перезатираются). Уже существующие
 * целевые файлы не трогаются.
 */
public final class LegacyPluginImport {

    private static final Logger LOG = LoggerFactory.getLogger("GoidaChat/Migration");

    /** Все сохраняемые JSON-файлы (имена идентичны в плагине и моде). */
    private static final String[] FILES = {
            "bans.json",          // блокировки (UUID/IP/HWID)
            "mutes.json",         // муты
            "ban_attempts.json",  // лог попыток входа забаненных (/banlog)
            "ignores.json",       // игноры
            "history.json",       // история ников/IP игроков
            "trusted.json",       // доверенные аккаунты
            "violations.json",    // нарушения
            "escalation.json"     // состояние авто-эскалации наказаний
    };

    private LegacyPluginImport() {}

    /**
     * @param configDir каталог конфигурации мода (FMLPaths.CONFIGDIR);
     *                  цель — {@code <configDir>/goidachat}
     * @param gameDir   корень сервера (FMLPaths.GAMEDIR);
     *                  источник — {@code <gameDir>/plugins/GoidaChat/goidachat}
     */
    public static void run(Path configDir, Path gameDir) {
        Path target = configDir.resolve("goidachat");
        Path marker = target.resolve(".migrated");
        try {
            if (Files.exists(marker)) return; // уже мигрировано — выходим

            Files.createDirectories(target);
            Path source = gameDir.resolve("plugins").resolve("GoidaChat").resolve("goidachat");

            int copied = 0;
            if (Files.isDirectory(source)) {
                for (String name : FILES) {
                    Path src = source.resolve(name);
                    Path dst = target.resolve(name);
                    if (Files.exists(src) && Files.notExists(dst)) {
                        Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES);
                        copied++;
                    }
                }
                if (copied > 0)
                    LOG.info("Перенесено файлов данных плагина GoidaChat: {} (из {})", copied, source);
            }

            Files.writeString(marker, "GoidaChat plugin -> mod data import done\n");
        } catch (Exception e) {
            LOG.warn("Импорт данных плагина GoidaChat не удался (продолжаем с нативными данными)", e);
        }
    }
}
