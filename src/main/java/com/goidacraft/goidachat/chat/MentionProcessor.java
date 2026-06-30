package com.goidacraft.goidachat.chat;

import com.goidacraft.goidachat.config.PluginConfig;
import com.goidacraft.goidachat.data.AdminRestManager;
import com.goidacraft.goidachat.data.PlayerSessionCache;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.*;

/**
 * Подсветка упоминаний по точному совпадению ника (без '@'). Сборка компонента и звук разделены:
 * звук играем только тем, кого реально упомянули и кто его не отключил.
 */
public final class MentionProcessor {

    public record Result(MutableComponent component, Set<UUID> mentioned) {}

    private MentionProcessor() {}

    public static Result process(String message) {
        Set<String> onlineNicks = PlayerSessionCache.getOnlineNicksLower();
        Set<UUID> mentioned = new HashSet<>();

        String[] words = message.split("(?<=\\s)|(?=\\s)");
        MutableComponent result = Component.literal("");

        for (String word : words) {
            String lower = word.trim().toLowerCase();
            UUID uid = PlayerSessionCache.getUuid(lower);
            if (!lower.isBlank() && onlineNicks.contains(lower) && uid != null) {
                result = result.append(Component.literal(word.trim())
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));
                mentioned.add(uid);
            } else {
                result = result.append(Component.literal(word));
            }
        }
        return new Result(result, mentioned);
    }

    /** Проигрывает звук упоминания тем получателям, кого упомянули и кто не отключил звук. */
    public static void playMentionSounds(Result result, Collection<ServerPlayer> recipients) {
        if (result.mentioned().isEmpty()) return;

        ResourceLocation soundLoc;
        try {
            soundLoc = ResourceLocation.parse(PluginConfig.mentionSound());
        } catch (Exception e) {
            soundLoc = ResourceLocation.parse("minecraft:entity.experience_orb.pickup");
        }
        SoundEvent sound = SoundEvent.createVariableRangeEvent(soundLoc);

        for (ServerPlayer player : recipients) {
            if (result.mentioned().contains(player.getUUID())
                    && !PlayerSessionCache.isMentionSoundDisabled(player.getUUID())
                    && !(AdminRestManager.isActive() && AdminRestManager.isAdmin(player))) {
                try {
                    player.playNotifySound(sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                } catch (Exception ignored) {}
            }
        }
    }
}
