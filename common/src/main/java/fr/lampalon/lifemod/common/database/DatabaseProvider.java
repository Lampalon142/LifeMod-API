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

    // Reports
    int saveReport(Report report);
    Report getReportById(int id);
    List<Report> getAllReports(int limit, int offset);
    List<Report> getReportsByStatus(String status, int limit, int offset);
    void updateReportStatus(int id, String status, UUID assignedTo);
    void addEvidence(ReportEvidence evidence);
    List<ReportEvidence> getEvidence(int reportId);

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
