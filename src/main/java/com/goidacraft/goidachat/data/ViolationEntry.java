package com.goidacraft.goidachat.data;

import java.util.UUID;

public class ViolationEntry {
    public String        id;
    public UUID          playerUuid;
    public String        playerName;
    public ViolationType type;
    public String        details;
    public String        reason;
    public long          timestamp;

    public ViolationEntry(String id, UUID playerUuid, String playerName,
                          ViolationType type, String details, String reason) {
        this.id         = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.type       = type;
        this.details    = details;
        this.reason     = reason;
        this.timestamp  = System.currentTimeMillis();
    }
}
