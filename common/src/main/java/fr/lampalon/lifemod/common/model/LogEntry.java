package fr.lampalon.lifemod.common.model;

import java.util.UUID;

public class LogEntry {
    private final long id;
    private final int type;
    private final UUID playerUuid;
    private final String playerName;
    private final UUID targetUuid;
    private final String targetName;
    private final String actionData;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String serverName;
    private final long createdAt;

    private LogEntry(Builder b) {
        this.id = b.id;
        this.type = b.type;
        this.playerUuid = b.playerUuid;
        this.playerName = b.playerName;
        this.targetUuid = b.targetUuid;
        this.targetName = b.targetName;
        this.actionData = b.actionData;
        this.world = b.world;
        this.x = b.x;
        this.y = b.y;
        this.z = b.z;
        this.serverName = b.serverName;
        this.createdAt = b.createdAt;
    }

    public long getId() { return id; }
    public int getType() { return type; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public String getActionData() { return actionData; }
    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getServerName() { return serverName; }
    public long getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long id;
        private int type;
        private UUID playerUuid;
        private String playerName;
        private UUID targetUuid;
        private String targetName;
        private String actionData;
        private String world;
        private int x;
        private int y;
        private int z;
        private String serverName;
        private long createdAt;

        public Builder id(long id) { this.id = id; return this; }
        public Builder type(LogType type) { this.type = type.ordinal(); return this; }
        public Builder type(int type) { this.type = type; return this; }
        public Builder playerUuid(UUID playerUuid) { this.playerUuid = playerUuid; return this; }
        public Builder playerName(String playerName) { this.playerName = playerName; return this; }
        public Builder targetUuid(UUID targetUuid) { this.targetUuid = targetUuid; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder actionData(String actionData) { this.actionData = actionData; return this; }
        public Builder world(String world) { this.world = world; return this; }
        public Builder x(int x) { this.x = x; return this; }
        public Builder y(int y) { this.y = y; return this; }
        public Builder z(int z) { this.z = z; return this; }
        public Builder serverName(String serverName) { this.serverName = serverName; return this; }
        public Builder createdAt(long createdAt) { this.createdAt = createdAt; return this; }
        public Builder now() { this.createdAt = System.currentTimeMillis(); return this; }

        public LogEntry build() {
            if (createdAt == 0) createdAt = System.currentTimeMillis();
            return new LogEntry(this);
        }
    }
}
