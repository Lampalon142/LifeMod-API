package fr.lampalon.lifemod.common.model;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String lastName;
    private String lastIp;
    private long lastSeen;

    public PlayerData(UUID uuid, String lastName, String lastIp, long lastSeen) {
        this.uuid = uuid;
        this.lastName = lastName;
        this.lastIp = lastIp;
        this.lastSeen = lastSeen;
    }

    public UUID getUuid() { return uuid; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}

