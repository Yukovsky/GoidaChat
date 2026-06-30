package com.goidacraft.goidachat.data;

import java.util.UUID;

public class MuteEntry {
    public UUID    uuid;
    public String  playerName;
    public String  reason;
    public long    expiresAt;
    public String  mutedBy;
    /** true — мут глушит ещё и голосовой чат (Simple Voice Chat), а не только текст. */
    public boolean voice;

    public MuteEntry(UUID uuid, String playerName, String reason, long expiresAt, String mutedBy) {
        this(uuid, playerName, reason, expiresAt, mutedBy, false);
    }

    public MuteEntry(UUID uuid, String playerName, String reason, long expiresAt, String mutedBy, boolean voice) {
        this.uuid       = uuid;
        this.playerName = playerName;
        this.reason     = reason;
        this.expiresAt  = expiresAt;
        this.mutedBy    = mutedBy;
        this.voice      = voice;
    }

    public boolean isExpired() {
        return expiresAt != Long.MAX_VALUE && System.currentTimeMillis() > expiresAt;
    }
}
