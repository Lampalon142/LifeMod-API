package fr.lampalon.lifemod.common.model;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String lastName;
    private String lastIp;
    private long lastSeen;
    private long firstSeen;
    private int sessionCount;
    private boolean inStaffMode;

    public PlayerData(UUID uuid, String lastName, String lastIp, long lastSeen, long firstSeen, int sessionCount, boolean inStaffMode) {
        this.uuid = uuid;
        this.lastName = lastName;
        this.lastIp = lastIp;
        this.lastSeen = lastSeen;
        this.firstSeen = firstSeen;
        this.sessionCount = sessionCount;
        this.inStaffMode = inStaffMode;
    }

    public UUID getUuid() { return uuid; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(long firstSeen) { this.firstSeen = firstSeen; }
    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
    public boolean isInStaffMode() { return inStaffMode; }
    public void setInStaffMode(boolean inStaffMode) { this.inStaffMode = inStaffMode; }
}

