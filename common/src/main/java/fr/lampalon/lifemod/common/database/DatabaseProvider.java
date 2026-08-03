package fr.lampalon.lifemod.common.database;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogQuery;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportEvidence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DatabaseProvider {
    void setupDatabase();
    Connection getConnection() throws SQLException;
    void closeConnection();

    /**
     * Whether replay blobs may be written from a background thread.
     * Pooled providers (e.g. MySQL/HikariCP) return true; providers backed by a
     * single shared connection (SQLite) return false so writes stay on the caller thread.
     */
    default boolean supportsAsyncWrites() {
        return false;
    }

    // Reports
    int saveReport(Report report);
    Report getReportById(int id);
    List<Report> getAllReports(int limit, int offset);
    List<Report> getReportsByStatus(String status, int limit, int offset);
    void updateReportStatus(int id, String status, UUID assignedTo);
    void addEvidence(ReportEvidence evidence);
    List<ReportEvidence> getEvidence(int reportId);
    void setReportReplayId(int reportId, String replayId);

    void saveRawInventory(UUID uuid, String serverName, byte[] data);
    byte[] getRawInventory(UUID uuid, String serverName);
    byte[] getRawInventory(UUID uuid);
    void deleteRawInventory(UUID uuid, String serverName);
    void deleteRawInventory(UUID uuid);
    
    // Simple coordinate storage
    void saveCoords(UUID uuid, String world, double x, double y, double z, float yaw, float pitch);
    StoredLocation getCoords(UUID uuid);

    // Sanctions
    void saveSanction(Sanction sanction);
    void updateSanction(Sanction sanction);
    void deleteSanction(UUID uuid);
    void cleanupExpiredSanctions();
    List<Sanction> getSanctions(UUID playerUuid);
    List<Sanction> getSanctionsIssuedBy(String issuerName, UUID issuerUuid);
    List<Sanction> getActiveSanctions(UUID playerUuid);
    Sanction getActiveSanction(UUID playerUuid, String playerName, SanctionType type);
    List<Sanction> getActiveSanctions(Collection<UUID> playerUuids, SanctionType type);

    // Player Data & Alts
    void savePlayerData(PlayerData data);
    PlayerData getPlayerData(UUID uuid);
    List<PlayerData> getAlts(String ip);
    int getLegitimateAccountCount(String ip);

    // Anti-Alt Sessions & Reputation
    void logAltSession(UUID uuid, String ip, String subnet, long connectedAt, int scoreAtLogin, boolean vpnDetected, String flags);
    void updateIPReputation(String ip, String subnet, int legitimateAccounts, int bannedAccounts, long lastUpdated, boolean natSuspected);
    IPReputation getIPReputation(String ip);

    // AntiVPN Cache
    void saveIPInfo(String ip, String countryCode, String countryName, String isp, boolean isProxy, long lastUpdate);
    fr.lampalon.lifemod.common.antivpn.data.IPInfo getIPInfo(String ip);
    void deleteExpiredIPInfo(long threshold);

    // Replays
    void saveReplay(String sessionName, UUID playerUuid, int entityId, String playerName, String worldName,
                    double startX, double startY, double startZ, float startYaw, float startPitch,
                    long durationMs, int frameCount, byte[] data);
    List<ReplayMeta> listReplays(int limit, int offset);
    ReplayMeta getReplayMeta(String sessionName);
    byte[] getReplayData(String sessionName);
    void deleteReplay(String sessionName);
    void deleteExpiredReplays(long thresholdMs);
    void markReplayAsReport(String sessionName);

    class ReplayMeta {
        public final String sessionName;
        public final UUID playerUuid;
        public final int entityId;
        public final String playerName;
        public final String worldName;
        public final double startX, startY, startZ;
        public final float startYaw, startPitch;
        public final long durationMs;
        public final int frameCount;
        public final long createdAt;
        public final boolean isReport;

        public ReplayMeta(String sessionName, UUID playerUuid, int entityId, String playerName, String worldName,
                          double startX, double startY, double startZ, float startYaw, float startPitch,
                          long durationMs, int frameCount, long createdAt, boolean isReport) {
            this.sessionName = sessionName; this.playerUuid = playerUuid; this.entityId = entityId;
            this.playerName = playerName; this.worldName = worldName;
            this.startX = startX; this.startY = startY; this.startZ = startZ;
            this.startYaw = startYaw; this.startPitch = startPitch;
            this.durationMs = durationMs; this.frameCount = frameCount;
            this.createdAt = createdAt; this.isReport = isReport;
        }
    }

    // Action Logs
    void saveLogBatch(List<LogEntry> entries);
    List<LogEntry> queryLogs(LogQuery query);
    long countLogs(LogQuery query);
    void purgeLogs(Map<Integer, Long> retentionMsPerType, long defaultRetentionMs);

    class IPReputation {
        public String ip;
        public String subnet;
        public int legitimateAccounts;
        public int bannedAccounts;
        public long lastUpdated;
        public boolean natSuspected;

        public IPReputation(String ip, String subnet, int legitimateAccounts, int bannedAccounts, long lastUpdated, boolean natSuspected) {
            this.ip = ip; this.subnet = subnet; this.legitimateAccounts = legitimateAccounts; 
            this.bannedAccounts = bannedAccounts; this.lastUpdated = lastUpdated; this.natSuspected = natSuspected;
        }
    }

    class StoredLocation {
        public String world;
        public double x, y, z;
        public float yaw, pitch;
        public StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
    }
}
