package com.goidacraft.goidachat.vote;

import com.goidacraft.goidachat.data.MuteEntry;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.util.ColorUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.goidacraft.goidavote.api.PollResult;
import ru.goidacraft.goidavote.api.VoteApi;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Голосование за мут/размут через GoidaVote. Полностью на NeoForge и событийно: вместо опроса
 * статуса опроса в тике, мы регистрируем callback завершения у GoidaVote (см. {@link VoteApi}),
 * который вызывается ровно один раз по окончании голосования. Класс инстанцируется только когда
 * GoidaVote установлен (см. {@code GoidaChat.onServerStarted}).
 */
public final class VoteMuteManager {

    public static final int VOTE_DURATION_SECONDS = 120;
    private static final double THRESHOLD   = 0.66;
    private static final long   COOLDOWN_MS = 12L * 60 * 60 * 1000L;

    private final MinecraftServer server;
    private final java.util.Set<UUID> activeTargets = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> muteCooldowns   = new ConcurrentHashMap<>();
    private final Map<UUID, Long> unmuteCooldowns = new ConcurrentHashMap<>();

    public VoteMuteManager(MinecraftServer server) {
        this.server = server;
    }

    /** GoidaVote готов принимать программные опросы. */
    public boolean isAvailable() {
        return VoteApi.isAvailable();
    }

    public boolean isTargetInActiveVote(UUID targetId) {
        return activeTargets.contains(targetId);
    }

    public boolean hasMuteCooldown(UUID id)   { return hasCd(muteCooldowns, id); }
    public boolean hasUnmuteCooldown(UUID id) { return hasCd(unmuteCooldowns, id); }
    public long getMuteCooldownRemaining(UUID id)   { return cdRemaining(muteCooldowns, id); }
    public long getUnmuteCooldownRemaining(UUID id) { return cdRemaining(unmuteCooldowns, id); }

    // ---------------------------------------------------------------- start votes

    public void startMuteVote(ServerPlayer initiator, UUID targetId, String targetName,
                              long muteDurationMs, String durationLabel) {
        muteCooldowns.put(initiator.getUUID(), System.currentTimeMillis());
        activeTargets.add(targetId);

        String pollId = VoteApi.createPoll(initiator,
                "Мутим " + targetName + " на " + durationLabel + "?",
                List.of("Да", "Нет"), VOTE_DURATION_SECONDS, true,
                result -> server.execute(() -> {
                    activeTargets.remove(targetId);
                    processMuteResult(result, targetId, targetName, muteDurationMs, durationLabel);
                }));

        if (pollId == null) {
            activeTargets.remove(targetId);
            muteCooldowns.remove(initiator.getUUID()); // голосование не стартовало — не наказываем кулдауном
            initiator.sendSystemMessage(ColorUtil.parse("&cНе удалось запустить голосование (GoidaVote недоступен)."));
        }
    }

    public void startUnmuteVote(ServerPlayer initiator, UUID targetId, String targetName) {
        unmuteCooldowns.put(initiator.getUUID(), System.currentTimeMillis());
        activeTargets.add(targetId);

        String pollId = VoteApi.createPoll(initiator,
                "Снимаем мут с " + targetName + "?",
                List.of("Да", "Нет"), VOTE_DURATION_SECONDS, true,
                result -> server.execute(() -> {
                    activeTargets.remove(targetId);
                    processUnmuteResult(result, targetId, targetName);
                }));

        if (pollId == null) {
            activeTargets.remove(targetId);
            unmuteCooldowns.remove(initiator.getUUID()); // голосование не стартовало — не наказываем кулдауном
            initiator.sendSystemMessage(ColorUtil.parse("&cНе удалось запустить голосование (GoidaVote недоступен)."));
        }
    }

    // ---------------------------------------------------------------- results

    private void processMuteResult(PollResult result, UUID targetId, String targetName,
                                   long muteDurationMs, String durationLabel) {
        double pct = result.fractionFor(0);
        int pctInt = (int) Math.round(pct * 100);
        boolean passed = !result.cancelled() && pct >= THRESHOLD;

        if (passed) {
            long expiresAt = System.currentTimeMillis() + muteDurationMs;
            PunishmentStorage.addMute(new MuteEntry(targetId, targetName,
                    "Голосование игроков", expiresAt, "VoteMute"));
            broadcast(ColorUtil.parse("&c[VoteMute] Игрок &e" + targetName
                    + " &cзаглушён голосованием на &f" + durationLabel + "&c! (" + pctInt + "% «Да»)"));
            ServerPlayer t = server.getPlayerList().getPlayer(targetId);
            if (t != null) t.sendSystemMessage(ColorUtil.parse(
                    "&cВы заглушены по результатам голосования на &f" + durationLabel + "&c."));
        } else {
            broadcast(ColorUtil.parse("&7[VoteMute] Мут &e" + targetName
                    + " &7не набрал 66%. (" + pctInt + "% «Да»)"));
        }
    }

    private void processUnmuteResult(PollResult result, UUID targetId, String targetName) {
        double pct = result.fractionFor(0);
        int pctInt = (int) Math.round(pct * 100);
        boolean passed = !result.cancelled() && pct >= THRESHOLD;

        if (passed) {
            if (PunishmentStorage.getMute(targetId) == null) {
                broadcast(ColorUtil.parse("&7[VoteMute] Снятие мута &e" + targetName
                        + " &7одобрено (" + pctInt + "% «Да»), но игрок уже не замучен."));
                return;
            }
            PunishmentStorage.removeMute(targetId);
            broadcast(ColorUtil.parse("&a[VoteMute] Мут игрока &e" + targetName
                    + " &aснят голосованием! (" + pctInt + "% «Да»)"));
            ServerPlayer t = server.getPlayerList().getPlayer(targetId);
            if (t != null) t.sendSystemMessage(ColorUtil.parse("&aВаш мут снят по результатам голосования."));
        } else {
            broadcast(ColorUtil.parse("&7[VoteMute] Снятие мута &e" + targetName
                    + " &7не набрало 66%. (" + pctInt + "% «Да»)"));
        }
    }

    // ---------------------------------------------------------------- helpers

    private void broadcast(Component msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(msg);
    }

    private boolean hasCd(Map<UUID, Long> map, UUID id) {
        Long t = map.get(id);
        return t != null && System.currentTimeMillis() - t < COOLDOWN_MS;
    }

    private long cdRemaining(Map<UUID, Long> map, UUID id) {
        Long t = map.get(id);
        if (t == null) return 0;
        return Math.max(0, COOLDOWN_MS - (System.currentTimeMillis() - t));
    }
}
