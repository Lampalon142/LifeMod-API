package fr.lampalon.lifemod.api.player;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final String lastName;
    private final String lastIp;
    private final long lastSeen;
    private final long firstSeen;
    private final int sessionCount;
    private final boolean inStaffMode;

    public PlayerData(UUID uuid, String lastName, String lastIp, long lastSeen,
                      long firstSeen, int sessionCount, boolean inStaffMode) {
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
    public String getLastIp() { return lastIp; }
    public long getLastSeen() { return lastSeen; }
    public long getFirstSeen() { return firstSeen; }
    public int getSessionCount() { return sessionCount; }
    public boolean isInStaffMode() { return inStaffMode; }
}