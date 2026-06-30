package com.goidacraft.goidachat.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Удаляет ванильные команды из диспетчера, чтобы наши одноимённые версии
 * (/msg, /tell, /w) не конфликтовали с ванильными и гарантированно
 * использовали форматирование, соцшпион и игнор-листы GoidaChat.
 *
 * Brigadier не предоставляет публичного API удаления, поэтому используется
 * рефлексия. При неудаче мод продолжает работу (ванильные команды останутся).
 */
final class VanillaCommandUtil {

    private static final Logger LOGGER = LogManager.getLogger("GoidaChat");

    private VanillaCommandUtil() {}

    static void remove(CommandDispatcher<CommandSourceStack> dispatcher, String... names) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        for (String name : names) {
            removeChild(root, name);
        }
    }

    private static void removeChild(CommandNode<CommandSourceStack> node, String name) {
        for (String fieldName : new String[]{"children", "literals", "arguments"}) {
            try {
                Field field = CommandNode.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(node);
                if (value instanceof Map<?, ?> map) {
                    map.remove(name);
                }
            } catch (NoSuchFieldException | IllegalAccessException | RuntimeException e) {
                LOGGER.warn("Не удалось убрать ванильную команду '{}' (поле {}): {}",
                        name, fieldName, e.toString());
            }
        }
    }
}
