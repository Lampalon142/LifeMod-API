package fr.lampalon.lifemod.platform.bukkit.managers.database.type;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.model.StaffNote;
import fr.lampalon.lifemod.common.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.*;

public class SQLiteManager implements DatabaseProvider {

    private final LifeMod plugin;
    private final DebugManager debug;
    private Connection connection;

    public SQLiteManager(LifeMod plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
        connect();
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setupDatabase() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reports (uuid TEXT PRIMARY KEY, reporter_uuid TEXT, target_uuid TEXT, reason TEXT, server_name TEXT, status TEXT, assigned_to TEXT, created_at INTEGER, updated_at INTEGER, closed_at INTEGER, close_reason TEXT, location_world TEXT, location_x REAL, location_y REAL, location_z REAL, location_yaw REAL, location_pitch REAL, last_updated_by TEXT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS report_staff_notes (note_id TEXT PRIMARY KEY, report_id TEXT NOT NULL, author TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, content TEXT NOT NULL, FOREIGN KEY (report_id) REFERENCES reports(uuid) ON DELETE CASCADE);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_inventories (uuid TEXT PRIMARY KEY, inventory_data TEXT NOT NULL, saved_at INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_coords (uuid TEXT PRIMARY KEY, world TEXT, x REAL, y REAL, z REAL, yaw REAL, pitch REAL, saved_at INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sanctions (uuid TEXT PRIMARY KEY, player_uuid TEXT, issuer_uuid TEXT, issuer_name TEXT, type TEXT, reason TEXT, created_at INTEGER, duration INTEGER, silent BOOLEAN, active BOOLEAN, evidence TEXT, removed_by_uuid TEXT, removed_by_name TEXT, remove_reason TEXT, removed_at INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid TEXT PRIMARY KEY, last_name TEXT, last_ip TEXT, last_seen INTEGER);");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) connect();
        return connection;
    }

    @Override
    public void closeConnection() {
        try { if (connection != null) connection.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public Report getReportByUuid(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM reports WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToReport(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void saveReport(Report report) {
        String sql = "INSERT OR REPLACE INTO reports (uuid, reporter_uuid, target_uuid, reason, server_name, status, assigned_to, created_at, updated_at, closed_at, close_reason, location_world, location_x, location_y, location_z, location_yaw, location_pitch, last_updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, report.getUuid().toString());
            stmt.setString(2, report.getReporterUuid().toString());
            stmt.setString(3, report.getTargetUuid().toString());
            stmt.setString(4, report.getReason());
            stmt.setString(5, report.getServerName());
            stmt.setString(6, report.getStatus().name());
            stmt.setString(7, report.getAssignedTo() != null ? report.getAssignedTo().toString() : null);
            stmt.setLong(8, report.getCreatedAt());
            stmt.setLong(9, report.getUpdatedAt());
            stmt.setLong(10, report.getClosedAt());
            stmt.setString(11, report.getCloseReason());
            stmt.setString(12, report.getLocationWorld());
            stmt.setDouble(13, report.getX());
            stmt.setDouble(14, report.getY());
            stmt.setDouble(15, report.getZ());
            stmt.setFloat(16, 0f);
            stmt.setFloat(17, 0f);
            stmt.setString(18, report.getLastUpdatedBy() != null ? report.getLastUpdatedBy().toString() : null);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateReport(Report report) { saveReport(report); }

    @Override
    public List<Report> getAllReports() {
        List<Report> reports = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM reports ORDER BY created_at DESC")) {
            while (rs.next()) reports.add(mapResultSetToReport(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    @Override
    public List<Report> getReportsByTarget(UUID targetUuid) {
        List<Report> reports = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM reports WHERE target_uuid = ? ORDER BY created_at DESC")) {
            ps.setString(1, targetUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    @Override
    public void deleteReport(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM reports WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<StaffNote> getStaffNotesForReport(UUID reportId) {
        List<StaffNote> notes = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM report_staff_notes WHERE report_id = ? ORDER BY created_at ASC")) {
            ps.setString(1, reportId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(new StaffNote(UUID.fromString(rs.getString("note_id")), UUID.fromString(rs.getString("author")), rs.getLong("created_at"), rs.getLong("updated_at"), rs.getString("content")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return notes;
    }

    @Override
    public void addStaffNote(UUID reportId, StaffNote note) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO report_staff_notes (note_id, report_id, author, created_at, updated_at, content) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, note.getNoteId().toString());
            ps.setString(2, reportId.toString());
            ps.setString(3, note.getAuthor().toString());
            ps.setLong(4, note.getCreatedAt());
            ps.setLong(5, note.getUpdatedAt());
            ps.setString(6, note.getContent());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void deleteStaffNote(UUID noteId) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM report_staff_notes WHERE note_id = ?")) {
            ps.setString(1, noteId.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateStaffNote(StaffNote note) {
        try (PreparedStatement ps = getConnection().prepareStatement("UPDATE report_staff_notes SET updated_at = ?, content = ? WHERE note_id = ?")) {
            ps.setLong(1, note.getUpdatedAt());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getNoteId().toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void savePlayerInventory(UUID uuid, Inventory inventory) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO player_inventories (uuid, inventory_data, saved_at) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serializeInventory(inventory));
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public ItemStack[] getPlayerInventory(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT inventory_data FROM player_inventories WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return deserializeInventory(rs.getString("inventory_data"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void savePlayerCoords(UUID uuid, Location loc) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO player_coords (uuid, world, x, y, z, yaw, pitch, saved_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, loc.getWorld().getName());
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw());
            ps.setFloat(7, loc.getPitch());
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public Location getPlayerCoords(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_coords WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Location(Bukkit.getWorld(rs.getString("world")), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void saveSanction(Sanction sanction) {
        String sql = "INSERT OR REPLACE INTO sanctions (uuid, player_uuid, issuer_uuid, issuer_name, type, reason, created_at, duration, silent, active, evidence, removed_by_uuid, removed_by_name, remove_reason, removed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, sanction.getUuid().toString());
            ps.setString(2, sanction.getPlayerUuid().toString());
            ps.setString(3, sanction.getIssuerUuid() != null ? sanction.getIssuerUuid().toString() : null);
            ps.setString(4, sanction.getIssuerName());
            ps.setString(5, sanction.getType().name());
            ps.setString(6, sanction.getReason());
            ps.setLong(7, sanction.getCreatedAt());
            ps.setLong(8, sanction.getDuration());
            ps.setBoolean(9, sanction.isSilent());
            ps.setBoolean(10, sanction.isActive());
            ps.setString(11, sanction.getEvidence());
            ps.setString(12, sanction.getRemovedByUuid() != null ? sanction.getRemovedByUuid().toString() : null);
            ps.setString(13, sanction.getRemovedByName());
            ps.setString(14, sanction.getRemoveReason());
            ps.setLong(15, sanction.getRemovedAt());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateSanction(Sanction s) { saveSanction(s); }

    @Override
    public List<Sanction> getSanctions(UUID playerUuid) {
        List<Sanction> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM sanctions WHERE player_uuid = ? ORDER BY created_at DESC")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToSanction(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<Sanction> getActiveSanctions(UUID playerUuid) {
        List<Sanction> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM sanctions WHERE player_uuid = ? AND active = 1")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sanction s = mapResultSetToSanction(rs);
                    if (!s.isExpired()) list.add(s);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public Sanction getActiveSanction(UUID playerUuid, SanctionType type) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM sanctions WHERE player_uuid = ? AND type = ? AND active = 1")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sanction s = mapResultSetToSanction(rs);
                    if (!s.isExpired()) return s;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void savePlayerData(PlayerData data) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO player_data (uuid, last_name, last_ip, last_seen) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getLastName());
            ps.setString(3, data.getLastIp());
            ps.setLong(4, data.getLastSeen());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new PlayerData(UUID.fromString(rs.getString("uuid")), rs.getString("last_name"), rs.getString("last_ip"), rs.getLong("last_seen"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<PlayerData> getAlts(String ip) {
        List<PlayerData> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_data WHERE last_ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new PlayerData(UUID.fromString(rs.getString("uuid")), rs.getString("last_name"), rs.getString("last_ip"), rs.getLong("last_seen")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Report report = new Report(UUID.fromString(rs.getString("uuid")), UUID.fromString(rs.getString("reporter_uuid")), UUID.fromString(rs.getString("target_uuid")), rs.getString("reason"), rs.getString("server_name"), ReportStatus.valueOf(rs.getString("status")), rs.getString("assigned_to") != null ? UUID.fromString(rs.getString("assigned_to")) : null, rs.getLong("created_at"), rs.getLong("updated_at"), rs.getString("last_updated_by") != null ? UUID.fromString(rs.getString("last_updated_by")) : null, rs.getLong("closed_at"), rs.getString("close_reason"));
        report.setLocation(rs.getString("location_world"), rs.getDouble("location_x"), rs.getDouble("location_y"), rs.getDouble("location_z"));
        return report;
    }

    private Sanction mapResultSetToSanction(ResultSet rs) throws SQLException {
        Sanction s = new Sanction(UUID.fromString(rs.getString("uuid")), UUID.fromString(rs.getString("player_uuid")), rs.getString("issuer_uuid") != null ? UUID.fromString(rs.getString("issuer_uuid")) : null, rs.getString("issuer_name"), SanctionType.valueOf(rs.getString("type")), rs.getString("reason"), rs.getLong("created_at"), rs.getLong("duration"), rs.getBoolean("silent"), rs.getBoolean("active"));
        s.setEvidence(rs.getString("evidence"));
        String rb = rs.getString("removed_by_uuid");
        if (rb != null) s.revoke(UUID.fromString(rb), rs.getString("removed_by_name"), rs.getString("remove_reason"));
        return s;
    }

    private String serializeInventory(Inventory inv) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream(); BukkitObjectOutputStream o = new BukkitObjectOutputStream(b)) {
            o.writeInt(inv.getSize());
            for (ItemStack i : inv.getContents()) o.writeObject(i);
            return Base64.getEncoder().encodeToString(b.toByteArray());
        } catch (Exception e) { return null; }
    }

    private ItemStack[] deserializeInventory(String s) {
        try (ByteArrayInputStream b = new ByteArrayInputStream(Base64.getDecoder().decode(s)); BukkitObjectInputStream i = new BukkitObjectInputStream(b)) {
            int size = i.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int x = 0; x < size; x++) items[x] = (ItemStack) i.readObject();
            return items;
        } catch (Exception e) { return new ItemStack[0]; }
    }
}

