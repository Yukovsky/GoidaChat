package com.goidacraft.goidachat.data;

/**
 * Одна запись блокировки. Несколько записей (UUID + IP + HWID) от одного /ban
 * имеют одинаковый banId, что позволяет снять их все одной командой.
 */
public class BanEntry {
    public String  banId;
    public BanType type;
    public String  target;
    public String  playerName;
    public String  reason;
    public long    expiresAt;
    public String  bannedBy;
    public long    createdAt;

    public BanEntry(String banId, BanType type, String target,
                    String playerName, String reason, long expiresAt, String bannedBy) {
        this.banId      = banId;
        this.type       = type;
        this.target     = target;
        this.playerName = playerName;
        this.reason     = reason;
        this.expiresAt  = expiresAt;
        this.bannedBy   = bannedBy;
        this.createdAt  = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return expiresAt != Long.MAX_VALUE && System.currentTimeMillis() > expiresAt;
    }
}
