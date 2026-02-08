package fr.lampalon.lifemod.common.model;

import java.util.UUID;

public class Sanction {
    private final UUID uuid; // ID unique de la sanction
    private final UUID playerUuid;
    private final String playerName;
    private final UUID issuerUuid; // UUID du staff (ou null si Console)
    private final String issuerName; // Nom du staff (pour historique rapide)
    private final String serverName;
    private final String category;
    private final SanctionType type;
    private final String reason;
    private final long createdAt;
    private final long duration; // 0 = Permanent
    private final boolean silent;
    private boolean active;
    private String evidence; // Lien preuve (optionnel)
    private UUID removedByUuid;
    private String removedByName;
    private String removeReason;
    private long removedAt;

    public Sanction(UUID uuid, UUID playerUuid, String playerName, UUID issuerUuid, String issuerName, String serverName, String category, SanctionType type, 
                    String reason, long createdAt, long duration, boolean silent, boolean active) {
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
    }
    
    // Constructeur simplifié pour création
    public Sanction(UUID playerUuid, String playerName, UUID issuerUuid, String issuerName, String serverName, String category, SanctionType type, String reason, long duration, boolean silent) {
        this(UUID.randomUUID(), playerUuid, playerName, issuerUuid, issuerName, serverName, category, type, reason, System.currentTimeMillis(), duration, silent, true);
    }

    public boolean isPermanent() {
        return duration == 0;
    }

    public boolean isExpired() {
        if (!active) return true;
        if (isPermanent()) return false;
        return System.currentTimeMillis() > (createdAt + duration);
    }
    
    public long getExpirationTime() {
        return isPermanent() ? -1 : createdAt + duration;
    }

    public void revoke(UUID removedByUuid, String removedByName, String removeReason) {
        this.active = false;
        this.removedByUuid = removedByUuid;
        this.removedByName = removedByName;
        this.removeReason = removeReason;
        this.removedAt = System.currentTimeMillis();
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    // Getters
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

