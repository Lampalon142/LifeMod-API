package fr.lampalon.lifemod.common.model;

import java.util.UUID;

public class LogQuery {
    private final LogType type;
    private final UUID playerUuid;
    private final UUID targetUuid;
    private final String world;
    private final long fromTime;
    private final long toTime;
    private final int limit;
    private final int offset;
    private final String serverName;

    private LogQuery(Builder b) {
        this.type = b.type;
        this.playerUuid = b.playerUuid;
        this.targetUuid = b.targetUuid;
        this.world = b.world;
        this.fromTime = b.fromTime;
        this.toTime = b.toTime;
        this.limit = b.limit;
        this.offset = b.offset;
        this.serverName = b.serverName;
    }

    public LogType getType() { return type; }
    public UUID getPlayerUuid() { return playerUuid; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getWorld() { return world; }
    public long getFromTime() { return fromTime; }
    public long getToTime() { return toTime; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
    public String getServerName() { return serverName; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private LogType type;
        private UUID playerUuid;
        private UUID targetUuid;
        private String world;
        private long fromTime;
        private long toTime;
        private int limit = 45;
        private int offset;
        private String serverName;

        public Builder type(LogType type) { this.type = type; return this; }
        public Builder playerUuid(UUID playerUuid) { this.playerUuid = playerUuid; return this; }
        public Builder targetUuid(UUID targetUuid) { this.targetUuid = targetUuid; return this; }
        public Builder world(String world) { this.world = world; return this; }
        public Builder fromTime(long fromTime) { this.fromTime = fromTime; return this; }
        public Builder toTime(long toTime) { this.toTime = toTime; return this; }
        public Builder limit(int limit) { this.limit = limit; return this; }
        public Builder offset(int offset) { this.offset = offset; return this; }
        public Builder serverName(String serverName) { this.serverName = serverName; return this; }

        public LogQuery build() {
            if (toTime == 0) toTime = Long.MAX_VALUE;
            return new LogQuery(this);
        }
    }
}
