package com.goidacraft.goidachat.events;

import com.goidacraft.goidachat.chat.AntiSpamManager;
import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.*;
import com.goidacraft.goidachat.util.BanScreens;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.HwidLookup;
import com.goidacraft.goidachat.util.IpUtil;
import com.goidacraft.goidachat.util.LuckPermsUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Вход/выход игроков: проверка банов (UUID → IP → HWID), логирование попыток обхода,
 * авто-бан евейдеров и обновление кэша/истории. На NeoForge событие входа приходит уже после
 * появления игрока в мире, поэтому забаненных отключаем через {@code connection.disconnect}.
 */
public class PlayerEventHandler {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        String name = player.getGameProfile().getName();
        String ip   = IpUtil.getIp(player);
        MinecraftServer server = player.getServer();

        // 1. Бан по UUID.
        BanEntry uuidBan = PunishmentStorage.getBanByUuid(uuid);
        if (uuidBan != null) {
            BanAttemptStorage.log(name, uuid.toString(), ip, null, "UUID", uuidBan.playerName);
            player.connection.disconnect(BanScreens.build(uuidBan));
            return;
        }

        // Доверенные аккаунты обходят IP/HWID баны.
        if (TrustedAccounts.contains(uuid)) {
            PlayerSessionCache.addPlayer(uuid, name);
            PlayerHistory.update(uuid, name, ip, null);
            return;
        }

        // 2. Бан по IP.
        if (PluginConfig.enableIpBan() && ip != null) {
            BanEntry ipBan = PunishmentStorage.getBanByIp(ip);
            if (ipBan != null) {
                BanAttemptStorage.log(name, uuid.toString(), ip, null, "IP", ipBan.playerName);
                notifyEvasion(server, name, "IP", ipBan);
                PlayerHistory.update(uuid, name, ip, null);
                if (PluginConfig.autoBanEvader()) {
                    PlayerRecord rec = PlayerHistory.get(uuid);
                    autoBanEvader(server, uuid, name, ip, rec != null ? rec.lastHwid : null, ipBan);
                }
                player.connection.disconnect(BanScreens.build(ipBan));
                return;
            }
        }

        // 3. Не забанен — кэш, история, отложенная проверка HWID.
        PlayerSessionCache.addPlayer(uuid, name);
        PlayerHistory.update(uuid, name, ip, null);

        if (PluginConfig.enableHwidBan() && HwidLookup.isAvailable() && server != null) {
            scheduleHwidCheck(server, uuid, name, ip);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        PlayerSessionCache.removePlayer(uuid);
        AntiSpamManager.remove(uuid);
    }

    // ---------------------------------------------------------------- HWID (async)

    private void scheduleHwidCheck(MinecraftServer server, UUID uuid, String name, String ip) {
        CompletableFuture.runAsync(() -> {
            if (TrustedAccounts.contains(uuid)) return;

            String hwid = null;
            for (int attempt = 0; attempt < 4 && hwid == null; attempt++) {
                if (attempt > 0) sleep(1000);
                hwid = HwidLookup.resolveHwid(uuid);
            }
            if (hwid == null) return;

            PlayerHistory.update(uuid, name, ip, hwid);

            BanEntry hwidBan = PunishmentStorage.getBanByHwid(hwid);
            if (hwidBan == null) return;

            final String finalHwid = hwid;
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) {
                    BanAttemptStorage.log(name, uuid.toString(), ip, finalHwid, "HWID", hwidBan.playerName);
                    if (PluginConfig.autoBanEvader()) {
                        autoBanEvader(server, uuid, name, ip, finalHwid, hwidBan);
                    }
                    notifyEvasion(server, name, "HWID", hwidBan);
                    p.connection.disconnect(BanScreens.build(hwidBan));
                }
            });
        });
    }

    // ---------------------------------------------------------------- auto-ban evaders

    /**
     * Присоединяет аккаунт-евейдер к ГРУППЕ оригинального бана (тот же banId), чтобы разбан
     * любого участника снимал всю группу. IP/HWID добавляются только если их ещё нет в группе.
     */
    private static void autoBanEvader(MinecraftServer server, UUID uuid, String name,
                                      String ip, String hwid, BanEntry originalBan) {
        if (PunishmentStorage.getBanByUuid(uuid) != null) return;

        String banId    = originalBan.banId;
        String bannedBy = originalBan.bannedBy + " (авто: обход " + originalBan.playerName + ")";
        List<BanEntry> entries = new ArrayList<>();
        List<String>   applied = new ArrayList<>();

        entries.add(new BanEntry(banId, BanType.UUID, uuid.toString(),
                name, originalBan.reason, originalBan.expiresAt, bannedBy));
        applied.add("UUID");

        if (PluginConfig.enableIpBan() && ip != null && PunishmentStorage.getBanByIp(ip) == null) {
            entries.add(new BanEntry(banId, BanType.IP, ip,
                    name, originalBan.reason, originalBan.expiresAt, bannedBy));
            applied.add("IP");
        }
        if (PluginConfig.enableHwidBan() && hwid != null && PunishmentStorage.getBanByHwid(hwid) == null) {
            entries.add(new BanEntry(banId, BanType.HWID, hwid,
                    name, originalBan.reason, originalBan.expiresAt, bannedBy));
            applied.add("HWID");
        }

        PunishmentStorage.addBans(entries);

        if (server == null) return;
        var msg = ColorUtil.parse("&4&l[Авто-бан] &e" + name + " &7присоединён к бану &e"
                + originalBan.playerName + " &7(" + String.join("+", applied)
                + "). Разбан любого участника снимет всю группу.");
        for (ServerPlayer staff : server.getPlayerList().getPlayers()) {
            if (LuckPermsUtil.hasPermission(staff, "goidachat.ban", 3)) staff.sendSystemMessage(msg);
        }
    }

    private static void notifyEvasion(MinecraftServer server, String evaderName, String via, BanEntry ban) {
        if (server == null || !PluginConfig.banEvadeNotify()) return;
        var msg = ColorUtil.parse("&c&l[Бан-евейд] &e" + evaderName
                + " &7пытался зайти (" + via + "). Совпадение с баном игрока &e" + ban.playerName + "&7.");
        for (ServerPlayer staff : server.getPlayerList().getPlayers()) {
            if (LuckPermsUtil.hasPermission(staff, "goidachat.ban", 3)) staff.sendSystemMessage(msg);
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
