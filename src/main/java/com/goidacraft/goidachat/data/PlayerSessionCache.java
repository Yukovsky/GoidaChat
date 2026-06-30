package com.goidacraft.goidachat.data;

import java.util.*;
import java.util.concurrent.*;

public final class PlayerSessionCache {

    private static final ConcurrentHashMap<UUID, String> uuidToName = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID>   lastMsgPartner = new ConcurrentHashMap<>();
    private static final Set<UUID> socialSpyEnabled = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> mentionSoundDisabled = ConcurrentHashMap.newKeySet();

    private PlayerSessionCache() {}

    public static void addPlayer(UUID uuid, String name) {
        uuidToName.put(uuid, name);
        nameToUuid.put(name.toLowerCase(), uuid);
    }

    public static void removePlayer(UUID uuid) {
        String name = uuidToName.remove(uuid);
        if (name != null) nameToUuid.remove(name.toLowerCase());
        lastMsgPartner.remove(uuid);
        socialSpyEnabled.remove(uuid);
        mentionSoundDisabled.remove(uuid);
    }

    public static UUID   getUuid(String name)        { return name == null ? null : nameToUuid.get(name.toLowerCase()); }
    public static String getName(UUID uuid)          { return uuidToName.get(uuid); }
    public static Collection<String> getAllNames()   { return uuidToName.values(); }
    public static Set<UUID> getOnlineUuids()         { return uuidToName.keySet(); }

    public static void setLastMsgPartner(UUID a, UUID b) {
        lastMsgPartner.put(a, b);
        lastMsgPartner.put(b, a);
    }
    public static UUID getLastMsgPartner(UUID uuid)  { return lastMsgPartner.get(uuid); }

    public static void toggleSocialSpy(UUID uuid) {
        if (!socialSpyEnabled.remove(uuid)) socialSpyEnabled.add(uuid);
    }
    public static boolean hasSocialSpy(UUID uuid)    { return socialSpyEnabled.contains(uuid); }

    public static void toggleMentionSound(UUID uuid) {
        if (!mentionSoundDisabled.remove(uuid)) mentionSoundDisabled.add(uuid);
    }
    public static boolean isMentionSoundDisabled(UUID uuid) { return mentionSoundDisabled.contains(uuid); }

    /** Множество ников онлайн-игроков в нижнем регистре — для O(1) проверки упоминаний. */
    public static Set<String> getOnlineNicksLower() {
        Set<String> result = new HashSet<>();
        uuidToName.values().forEach(n -> result.add(n.toLowerCase()));
        return result;
    }
}
