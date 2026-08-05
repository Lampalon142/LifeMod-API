package fr.lampalon.lifemod.api.replay;

import java.util.UUID;

public class ReplayInfo {

    private final String sessionName;
    private final UUID playerUuid;
    private final int entityId;
    private final String playerName;
    private final String worldName;
    private final long durationMs;
    private final int frameCount;
    private final long createdAt;
    private final boolean report;

    public ReplayInfo(String sessionName, UUID playerUuid, int entityId, String playerName,
                      String worldName, long durationMs, int frameCount, long createdAt, boolean report) {
        this.sessionName = sessionName;
        this.playerUuid = playerUuid;
        this.entityId = entityId;
        this.playerName = playerName;
        this.worldName = worldName;
        this.durationMs = durationMs;
        this.frameCount = frameCount;
        this.createdAt = createdAt;
        this.report = report;
    }

    public String getSessionName() { return sessionName; }
    public UUID getPlayerUuid() { return playerUuid; }
    public int getEntityId() { return entityId; }
    public String getPlayerName() { return playerName; }
    public String getWorldName() { return worldName; }
    public long getDurationMs() { return durationMs; }
    public int getFrameCount() { return frameCount; }
    public long getCreatedAt() { return createdAt; }
    public boolean isReport() { return report; }
}