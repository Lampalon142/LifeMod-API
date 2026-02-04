package fr.lampalon.lifemod.platform.bukkit

import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseProvider;.managers.database;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.StaffNote;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.model.PlayerData;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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

    void savePlayerInventory(UUID uuid, Inventory inventory);
    ItemStack[] getPlayerInventory(UUID uuid);
    void savePlayerCoords(UUID uuid, Location location);
    Location getPlayerCoords(UUID uuid);

    // Sanctions
    void saveSanction(Sanction sanction);
    void updateSanction(Sanction sanction);
    List<Sanction> getSanctions(UUID playerUuid);
    List<Sanction> getActiveSanctions(UUID playerUuid);
    Sanction getActiveSanction(UUID playerUuid, SanctionType type);

    // Player Data & Alts
    void savePlayerData(PlayerData data);
    PlayerData getPlayerData(UUID uuid);
    List<PlayerData> getAlts(String ip);
}

