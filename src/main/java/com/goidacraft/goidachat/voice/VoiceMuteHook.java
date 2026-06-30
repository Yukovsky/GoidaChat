package com.goidacraft.goidachat.voice;

import com.goidacraft.goidachat.data.MuteEntry;
import com.goidacraft.goidachat.data.PunishmentStorage;
import com.goidacraft.goidachat.util.ColorUtil;
import com.goidacraft.goidachat.util.TimeUtil;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Интеграция с Simple Voice Chat: глушит микрофон игрокам с голосовым мутом
 * ({@link MuteEntry#voice}). Обнаруживается самим модом SVC через ServiceLoader
 * (META-INF/services), поэтому класс загружается только при наличии voicechat —
 * никакой ручной регистрации и риска NoClassDefFoundError в его отсутствие.
 */
public final class VoiceMuteHook implements VoicechatPlugin {

    /** Не чаще одного уведомления над хотбаром в N мс — мик-пакеты идут десятки раз/сек. */
    private static final long ACTIONBAR_CD_MS = 3000L;
    private final ConcurrentHashMap<UUID, Long> lastNotice = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "goidachat";
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof ServerPlayer player)) return;

        MuteEntry mute = PunishmentStorage.getMute(player.getUUID());
        if (mute == null || !mute.voice) return;

        event.cancel();

        long now  = System.currentTimeMillis();
        Long last = lastNotice.get(player.getUUID());
        if (last == null || now - last >= ACTIONBAR_CD_MS) {
            lastNotice.put(player.getUUID(), now);
            player.displayClientMessage(ColorUtil.parse(
                    "&cВы заглушены в голосовом чате. &7Осталось: &f"
                    + TimeUtil.formatRemaining(mute.expiresAt)), true);
        }
    }
}
