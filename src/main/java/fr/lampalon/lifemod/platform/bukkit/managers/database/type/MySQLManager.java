package fr.lampalon.lifemod.platform.bukkit.managers.database.type;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.common.model.StaffNote;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class MySQLManager implements DatabaseProvider {

    private final LifeMod plugin;
    private HikariDataSource dataSource;

    public MySQLManager(LifeMod plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setupDatabase() {
        FileConfiguration config = this.plugin.getConfig();
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String database = config.getString("database.name");
        String user = config.getString("database.user");
        String password = config.getString("database.password");
        int poolsize = config.getInt("database.poolsize", 10);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolsize);
        hikariConfig.setPoolName("LifeMod-MySQL");
        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reports (uuid VARCHAR(36) PRIMARY KEY, reporter_uuid VARCHAR(36), target_uuid VARCHAR(36), reason TEXT, server_name VARCHAR(64), status VARCHAR(32), assigned_to VARCHAR(36), created_at BIGINT, updated_at BIGINT, closed_at BIGINT, close_reason TEXT, location_world VARCHAR(64), location_x DOUBLE, location_y DOUBLE, location_z DOUBLE, location_yaw FLOAT, location_pitch FLOAT, last_updated_by VARCHAR(36));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS report_staff_notes (note_id VARCHAR(36) PRIMARY KEY, report_id VARCHAR(36) NOT NULL, author VARCHAR(36) NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, content TEXT NOT NULL, FOREIGN KEY (report_id) REFERENCES reports(uuid));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_inventories (uuid VARCHAR(36) PRIMARY KEY, inventory_data LONGBLOB NOT NULL, saved_at BIGINT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_coords (uuid VARCHAR(36) PRIMARY KEY, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, saved_at BIGINT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sanctions (uuid VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36), issuer_uuid VARCHAR(36), issuer_name VARCHAR(32), type VARCHAR(16), reason TEXT, created_at BIGINT, duration BIGINT, silent BOOLEAN, active BOOLEAN, evidence TEXT, removed_by_uuid VARCHAR(36), removed_by_name VARCHAR(32), remove_reason TEXT, removed_at BIGINT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid VARCHAR(36) PRIMARY KEY, last_name VARCHAR(32), last_ip VARCHAR(45), last_seen BIGINT);");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void closeConnection() {
        if (dataSource != null) dataSource.close();
    }

    @Override
    public Report getReportByUuid(UUID uuid) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM reports WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToReport(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void saveReport(Report report) {
        String sql = "INSERT INTO reports (uuid, reporter_uuid, target_uuid, reason, server_name, status, assigned_to, created_at, updated_at, closed_at, close_reason, location_world, location_x, location_y, location_z, location_yaw, location_pitch, last_updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE status=VALUES(status), assigned_to=VALUES(assigned_to), updated_at=VALUES(updated_at), closed_at=VALUES(closed_at), close_reason=VALUES(close_reason), last_updated_by=VALUES(last_updated_by)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, report.getUuid().toString());
            ps.setString(2, report.getReporterUuid().toString());
            ps.setString(3, report.getTargetUuid().toString());
            ps.setString(4, report.getReason());
            ps.setString(5, report.getServerName());
            ps.setString(6, report.getStatus().name());
            ps.setString(7, report.getAssignedTo() != null ? report.getAssignedTo().toString() : null);
            ps.setLong(8, report.getCreatedAt());
            ps.setLong(9, report.getUpdatedAt());
            ps.setLong(10, report.getClosedAt());
            ps.setString(11, report.getCloseReason());
            ps.setString(12, report.getLocationWorld());
            ps.setDouble(13, report.getX());
            ps.setDouble(14, report.getY());
            ps.setDouble(15, report.getZ());
            ps.setFloat(16, 0f);
            ps.setFloat(17, 0f);
            ps.setString(18, report.getLastUpdatedBy() != null ? report.getLastUpdatedBy().toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateReport(Report report) { saveReport(report); }

    @Override
    public List<Report> getAllReports() {
        List<Report> reports = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM reports ORDER BY created_at DESC")) {
            while (rs.next()) reports.add(mapResultSetToReport(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    @Override
    public List<Report> getReportsByTarget(UUID targetUuid) {
        List<Report> reports = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM reports WHERE target_uuid = ? ORDER BY created_at DESC")) {
            ps.setString(1, targetUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    @Override
    public void deleteReport(UUID uuid) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM reports WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<StaffNote> getStaffNotesForReport(UUID reportId) {
        List<StaffNote> notes = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM report_staff_notes WHERE report_id = ? ORDER BY created_at ASC")) {
            ps.setString(1, reportId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(new StaffNote(UUID.fromString(rs.getString("note_id")), UUID.fromString(rs.getString("author")), rs.getLong("created_at"), rs.getLong("updated_at"), rs.getString("content")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return notes;
    }

    @Override
    public void addStaffNote(UUID reportId, StaffNote note) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO report_staff_notes (note_id, report_id, author, created_at, updated_at, content) VALUES (?, ?, ?, ?, ?, ?)")) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM report_staff_notes WHERE note_id = ?")) {
            ps.setString(1, noteId.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateStaffNote(StaffNote note) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE report_staff_notes SET updated_at = ?, content = ? WHERE note_id = ?")) {
            ps.setLong(1, note.getUpdatedAt());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getNoteId().toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void savePlayerInventory(UUID uuid, Inventory inventory) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("REPLACE INTO player_inventories (uuid, inventory_data, saved_at) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setBytes(2, serializeInventory(inventory));
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException | IOException e) { e.printStackTrace(); }
    }

    @Override
    public ItemStack[] getPlayerInventory(UUID uuid) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT inventory_data FROM player_inventories WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return deserializeInventory(rs.getBytes("inventory_data"));
            }
        } catch (SQLException | IOException | ClassNotFoundException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void savePlayerCoords(UUID uuid, Location loc) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("REPLACE INTO player_coords (uuid, world, x, y, z, yaw, pitch, saved_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_coords WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Location(Bukkit.getWorld(rs.getString("world")), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void saveSanction(Sanction sanction) {
        String sql = "INSERT INTO sanctions (uuid, player_uuid, issuer_uuid, issuer_name, type, reason, created_at, duration, silent, active, evidence, removed_by_uuid, removed_by_name, remove_reason, removed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE active=VALUES(active), evidence=VALUES(evidence), removed_by_uuid=VALUES(removed_by_uuid), removed_by_name=VALUES(removed_by_name), remove_reason=VALUES(remove_reason), removed_at=VALUES(removed_at)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM sanctions WHERE player_uuid = ? ORDER BY created_at DESC")) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM sanctions WHERE player_uuid = ? AND active = 1")) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM sanctions WHERE player_uuid = ? AND type = ? AND active = 1")) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO player_data (uuid, last_name, last_ip, last_seen) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_name=VALUES(last_name), last_ip=VALUES(last_ip), last_seen=VALUES(last_seen)")) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getLastName());
            ps.setString(3, data.getLastIp());
            ps.setLong(4, data.getLastSeen());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_data WHERE uuid = ?")) {
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
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_data WHERE last_ip = ?")) {
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

    private byte[] serializeInventory(Inventory inv) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream(); BukkitObjectOutputStream o = new BukkitObjectOutputStream(b)) {
            o.writeInt(inv.getSize());
            for (ItemStack i : inv.getContents()) o.writeObject(i);
            return b.toByteArray();
        }
    }

    private ItemStack[] deserializeInventory(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(data); BukkitObjectInputStream i = new BukkitObjectInputStream(b)) {
            int size = i.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int x = 0; x < size; x++) items[x] = (ItemStack) i.readObject();
            return items;
        }
    }
}

