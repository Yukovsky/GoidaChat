package com.goidacraft.goidachat.data;

public class BanAttemptEntry {
    public String evaderName;
    public String evaderUuid;
    public String ip;
    public String hwid;
    public String via;           // "UUID", "IP", "HWID"
    public String bannedPlayer;  // имя оригинально-забаненного
    public long   timestamp;

    public BanAttemptEntry(String evaderName, String evaderUuid,
                           String ip, String hwid, String via, String bannedPlayer) {
        this.evaderName   = evaderName;
        this.evaderUuid   = evaderUuid;
        this.ip           = ip;
        this.hwid         = hwid;
        this.via          = via;
        this.bannedPlayer = bannedPlayer;
        this.timestamp    = System.currentTimeMillis();
    }
}
