package fr.lampalon.lifemod.common.database;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.StaffNote;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.model.PlayerData;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface DatabaseProvider {
    void setupDatabase();
    Connection getConnection() throws SQLException;
    void closeConnection();

    Report getReportByUuid(UUID uuid);
    void saveReport(Report report);
    void updateReport(Report report);
    List<Report> getAllReports();
    List<Report> getReportsByTarget(UUID targetUuid);
    void deleteReport(UUID uuid);

    List<StaffNote> getStaffNotesForReport(UUID reportId);
    void addStaffNote(UUID reportId, StaffNote note);
    void deleteStaffNote(UUID noteId);
    void updateStaffNote(StaffNote note);

    void saveRawInventory(UUID uuid, byte[] data);
    byte[] getRawInventory(UUID uuid);
    
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

    // Player Data & Alts
    void savePlayerData(PlayerData data);
    PlayerData getPlayerData(UUID uuid);
    List<PlayerData> getAlts(String ip);

    class StoredLocation {
        public String world;
        public double x, y, z;
        public float yaw, pitch;
        public StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
    }
}
