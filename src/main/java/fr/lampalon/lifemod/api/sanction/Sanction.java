package fr.lampalon.lifemod.api.sanction;

import java.util.UUID;

public class Sanction {
    private final UUID uuid;
    private final UUID playerUuid;
    private final String playerName;
    private final UUID issuerUuid;
    private final String issuerName;
    private final String serverName;
    private final String category;
    private final SanctionType type;
    private final String reason;
    private final long createdAt;
    private final long duration;
    private final boolean silent;
    private final boolean active;
    private final String evidence;
    private final UUID removedByUuid;
    private final String removedByName;
    private final String removeReason;
    private final long removedAt;

    public Sanction(UUID uuid, UUID playerUuid, String playerName, UUID issuerUuid, String issuerName,
                    String serverName, String category, SanctionType type, String reason,
                    long createdAt, long duration, boolean silent, boolean active,
                    String evidence, UUID removedByUuid, String removedByName,
                    String removeReason, long removedAt) {
        this.uuid = uuid;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.issuerUuid = issuerUuid;
        this.issuerName = issuerName;
        this.serverName = serverName;
        this.category = category;
        this.type = type;
        this.reason = reason;
        this.createdAt = createdAt;
        this.duration = duration;
        this.silent = silent;
        this.active = active;
        this.evidence = evidence;
        this.removedByUuid = removedByUuid;
        this.removedByName = removedByName;
        this.removeReason = removeReason;
        this.removedAt = removedAt;
    }

    public boolean isPermanent() {
        return duration == 0;
    }

    public long getExpirationTime() {
        return isPermanent() ? -1 : createdAt + duration;
    }

    public UUID getUuid() { return uuid; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public UUID getIssuerUuid() { return issuerUuid; }
    public String getIssuerName() { return issuerName; }
    public String getServerName() { return serverName; }
    public String getCategory() { return category; }
    public SanctionType getType() { return type; }
    public String getReason() { return reason; }
    public long getCreatedAt() { return createdAt; }
    public long getDuration() { return duration; }
    public boolean isSilent() { return silent; }
    public boolean isActive() { return active; }
    public String getEvidence() { return evidence; }
    public UUID getRemovedByUuid() { return removedByUuid; }
    public String getRemovedByName() { return removedByName; }
    public String getRemoveReason() { return removeReason; }
    public long getRemovedAt() { return removedAt; }
}
