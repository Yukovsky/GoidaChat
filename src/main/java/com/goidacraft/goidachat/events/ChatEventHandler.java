package com.goidacraft.goidachat.events;

import com.goidacraft.goidachat.chat.AntiSpamManager;
import com.goidacraft.goidachat.chat.ChatRouter;
import com.goidacraft.goidachat.chat.ContentFilter;
import com.goidacraft.goidachat.chat.EscalationManager;
import com.goidacraft.goidachat.GoidaChat;
import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.*;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.TimeUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import ru.goidacraft.goidavote.api.PromptApi;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatEventHandler {

    /** Ванильные «чат-команды», которые должны быть запрещены замученному игроку. */
    private static final Set<String> MUTED_BLOCKED_COMMANDS =
            Set.of("me", "say", "msg", "tell", "w", "teammsg", "tm", "r", "ac");

    // Кулдаун записи нарушений по типу: не чаще 1 раза в 30 сек на игрока+тип
    private static final ConcurrentHashMap<String, Long> violationCooldowns = new ConcurrentHashMap<>();
    private static final long VIOLATION_CD_MS = 30_000L;

    private static boolean shouldRecord(UUID uuid, ViolationType type) {
        String key = uuid + ":" + type.name();
        long now  = System.currentTimeMillis();
        Long last = violationCooldowns.get(key);
        if (last != null && now - last < VIOLATION_CD_MS) return false;
        violationCooldowns.put(key, now);
        return true;
    }

    // HIGH (а не HIGHEST): мы намеренно на ступень ниже, чтобы листенеры на HIGHEST успели
    // отменить событие до нас, а мы кооперативно уступили (см. проверку isCanceled ниже). Так
    // гейт GoidaDI (HIGHEST) блокирует чат запертого игрока ДО того, как мы его разошлём, а
    // мастер голосований GoidaVote (HIGHEST) забирает ввод. HIGH всё ещё выше ванильной рассылки,
    // поэтому гарантия «отменить до широковещания» сохраняется.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        if (sender == null) return;

        // Если событие уже обработано (например, GoidaVote забрал ввод для мастера голосования,
        // либо гейт GoidaDI заблокировал чат запертого игрока) — не трогаем его. Дополнительно
        // кооперативно уступаем, если GoidaVote ждёт ввод от игрока.
        // PromptApi — класс GoidaVote (compileOnly); короткое замыкание по voteLoaded() гарантирует,
        // что при отсутствии мода ссылка на класс не разрешается (как в votemute).
        if (event.isCanceled()) return;
        if (GoidaChat.voteLoaded() && PromptApi.isAwaiting(sender.getUUID())) return;

        event.setCanceled(true);

        String raw = ColorUtil.sanitize(event.getRawText());
        if (raw.isEmpty()) return;

        if (isMuted(sender)) return;

        if (AntiSpamManager.isSpamming(sender)) {
            recordViolation(sender, ViolationType.FLOOD,
                    PluginConfig.spamMaxMessages() + " сообщений / " + PluginConfig.spamWindowMs() + "мс",
                    "Флуд сообщениями");
            sender.sendSystemMessage(ColorUtil.parse("&cВы отправляете сообщения слишком быстро."));
            return;
        }

        if (AntiSpamManager.isDuplicate(sender, raw)) {
            recordViolation(sender, ViolationType.DUPLICATE, clip(raw), "Повтор одинаковых сообщений");
            sender.sendSystemMessage(ColorUtil.parse("&cНе повторяйте одно и то же сообщение."));
            return;
        }

        if (AntiSpamManager.isCharRepeat(sender, raw)) {
            recordViolation(sender, ViolationType.CHAR_REPEAT, clip(raw), "Многократный повтор символа");
            sender.sendSystemMessage(ColorUtil.parse("&cНельзя многократно повторять один символ."));
            return;
        }

        // --- Контентные фильтры (реклама, мат, капслок) ---

        ContentFilter.Result adv = ContentFilter.checkAdvertising(sender, raw);
        if (adv != null) {
            recordViolation(sender, ViolationType.ADVERTISING, adv.details, "Реклама в чате");
            sender.sendSystemMessage(ColorUtil.parse("&cРеклама сторонних ресурсов запрещена."));
            return;
        }

        ContentFilter.Result prof = ContentFilter.checkProfanity(sender, raw);
        if (prof != null) {
            recordViolation(sender, ViolationType.PROFANITY, prof.details, "Запрещённые слова");
            if (prof.isBlock()) {
                sender.sendSystemMessage(ColorUtil.parse("&cСообщение содержит запрещённые слова."));
                return;
            }
            raw = prof.modified;
        }

        ContentFilter.Result caps = ContentFilter.checkCaps(sender, raw);
        if (caps != null) {
            recordViolation(sender, ViolationType.CAPS, caps.details, "Чрезмерный капслок");
            if (caps.isBlock()) {
                sender.sendSystemMessage(ColorUtil.parse("&cНе используйте много заглавных букв."));
                return;
            }
            raw = caps.modified;
        }

        List<ServerPlayer> allPlayers = sender.server.getPlayerList().getPlayers();

        String prefix = PluginConfig.globalPrefix();
        if (!prefix.isEmpty() && raw.startsWith(prefix)) {
            String globalText = raw.substring(prefix.length()).trim();
            if (!globalText.isEmpty()) ChatRouter.handleGlobal(sender, globalText, allPlayers);
        } else {
            ChatRouter.handleLocal(sender, raw, allPlayers);
        }
    }

    /** Блокирует ванильные чат-команды замученным игрокам. */
    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) input = input.substring(1);
        int space = input.indexOf(' ');
        String command = (space >= 0 ? input.substring(0, space) : input).toLowerCase();

        if (MUTED_BLOCKED_COMMANDS.contains(command) && isMuted(player)) {
            event.setCanceled(true);
        }
    }

    private static void recordViolation(ServerPlayer player, ViolationType type, String details, String reason) {
        if (shouldRecord(player.getUUID(), type)) {
            ViolationStorage.add(player.getUUID(), player.getGameProfile().getName(), type, details, reason);
            EscalationManager.apply(player, type);
        }
    }

    private static boolean isMuted(ServerPlayer player) {
        MuteEntry mute = PunishmentStorage.getMute(player.getUUID());
        if (mute == null) return false;
        player.sendSystemMessage(ColorUtil.parse(
                "&cВы заглушены. Причина: &f" + mute.reason
                + " &c| Осталось: &f" + TimeUtil.formatRemaining(mute.expiresAt)));
        return true;
    }

    private static String clip(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }
}
