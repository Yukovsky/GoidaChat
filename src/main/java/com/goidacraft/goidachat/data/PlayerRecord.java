package com.goidacraft.goidachat.data;

import java.util.UUID;

public class PlayerRecord {
    public UUID   uuid;
    public String name;
    public String lastIp;
    public String lastHwid;
    public long   lastSeen;

    public PlayerRecord(UUID uuid, String name, String lastIp, String lastHwid) {
        this.uuid     = uuid;
        this.name     = name;
        this.lastIp   = lastIp;
        this.lastHwid = lastHwid;
        this.lastSeen = System.currentTimeMillis();
    }
}
