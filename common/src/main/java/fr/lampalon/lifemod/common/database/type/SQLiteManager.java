package fr.lampalon.lifemod.common.database.type;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.*;
import fr.lampalon.lifemod.common.service.IConfigurationService;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class SQLiteManager implements DatabaseProvider {

    private Connection connection;
    private final Object lock = new Object();

    public SQLiteManager() {
        synchronized (lock) {
            connect();
        }
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File("plugins/LifeMod/database.db");
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
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_inventories (uuid TEXT, server_name TEXT, inventory_data TEXT NOT NULL, saved_at INTEGER, PRIMARY KEY (uuid, server_name));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_coords (uuid TEXT PRIMARY KEY, world TEXT, x REAL, y REAL, z REAL, yaw REAL, pitch REAL, saved_at INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sanctions (uuid TEXT PRIMARY KEY, player_uuid TEXT, player_name TEXT, issuer_uuid TEXT, issuer_name TEXT, server_name TEXT, category TEXT, type TEXT, reason TEXT, created_at INTEGER, duration INTEGER, silent BOOLEAN, active BOOLEAN, evidence TEXT, removed_by_uuid TEXT, removed_by_name TEXT, remove_reason TEXT, removed_at INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid TEXT PRIMARY KEY, last_name TEXT, last_ip TEXT, last_seen INTEGER, first_seen INTEGER, session_count INTEGER DEFAULT 0, in_staff_mode BOOLEAN DEFAULT 0);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS antivpn_cache (ip TEXT PRIMARY KEY, country_code TEXT, country_name TEXT, isp TEXT, is_proxy BOOLEAN, last_update INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS alt_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL, ip TEXT NOT NULL, subnet TEXT NOT NULL, connected_at INTEGER NOT NULL, score_at_login INTEGER DEFAULT 0, vpn_detected BOOLEAN DEFAULT 0, flags TEXT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ip_reputation (ip TEXT PRIMARY KEY, subnet TEXT NOT NULL, legitimate_accounts INTEGER DEFAULT 0, banned_accounts INTEGER DEFAULT 0, last_updated INTEGER, nat_suspected BOOLEAN DEFAULT 0);");

            // Migration pour les tables existantes
            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN in_staff_mode BOOLEAN DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN first_seen INTEGER;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN session_count INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_inventories ADD COLUMN server_name TEXT;"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        synchronized (lock) {
            if (connection == null || connection.isClosed()) connect();
        }
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
    public List<Report> getAllReports(int limit, int offset) {
        List<Report> reports = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM reports ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reports.add(mapResultSetToReport(rs));
            }
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
    public void saveRawInventory(UUID uuid, String serverName, byte[] data) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO player_inventories (uuid, server_name, inventory_data, saved_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serverName);
            ps.setBytes(3, data);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public byte[] getRawInventory(UUID uuid, String serverName) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT inventory_data FROM player_inventories WHERE uuid = ? AND server_name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serverName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes("inventory_data");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public byte[] getRawInventory(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT inventory_data FROM player_inventories WHERE uuid = ? ORDER BY saved_at DESC LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes("inventory_data");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void deleteRawInventory(UUID uuid, String serverName) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM player_inventories WHERE uuid = ? AND server_name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serverName);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void deleteRawInventory(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM player_inventories WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void saveCoords(UUID uuid, String world, double x, double y, double z, float yaw, float pitch) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO player_coords (uuid, world, x, y, z, yaw, pitch, saved_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, world);
            ps.setDouble(3, x);
            ps.setDouble(4, y);
            ps.setDouble(5, z);
            ps.setFloat(6, yaw);
            ps.setFloat(7, pitch);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public StoredLocation getCoords(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_coords WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new StoredLocation(rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void saveSanction(Sanction sanction) {
        String sql = "INSERT OR REPLACE INTO sanctions (uuid, player_uuid, player_name, issuer_uuid, issuer_name, server_name, category, type, reason, created_at, duration, silent, active, evidence, removed_by_uuid, removed_by_name, remove_reason, removed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, sanction.getUuid().toString());
            ps.setString(2, sanction.getPlayerUuid().toString());
            ps.setString(3, sanction.getPlayerName());
            ps.setString(4, sanction.getIssuerUuid() != null ? sanction.getIssuerUuid().toString() : null);
            ps.setString(5, sanction.getIssuerName());
            ps.setString(6, sanction.getServerName());
            ps.setString(7, sanction.getCategory());
            ps.setString(8, sanction.getType().name());
            ps.setString(9, sanction.getReason());
            ps.setLong(10, sanction.getCreatedAt());
            ps.setLong(11, sanction.getDuration());
            ps.setBoolean(12, sanction.isSilent());
            ps.setBoolean(13, sanction.isActive());
            ps.setString(14, sanction.getEvidence());
            ps.setString(15, sanction.getRemovedByUuid() != null ? sanction.getRemovedByUuid().toString() : null);
            ps.setString(16, sanction.getRemovedByName());
            ps.setString(17, sanction.getRemoveReason());
            ps.setLong(18, sanction.getRemovedAt());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateSanction(Sanction s) { saveSanction(s); }

    @Override
    public void deleteSanction(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM sanctions WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void cleanupExpiredSanctions() {
        String sql = "UPDATE sanctions SET active = 0 WHERE active = 1 AND duration > 0 AND (created_at + duration) < ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

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
    public List<Sanction> getSanctionsIssuedBy(String issuerName, UUID issuerUuid) {
        List<Sanction> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM sanctions WHERE issuer_uuid = ? OR issuer_name = ? ORDER BY created_at DESC")) {
            ps.setString(1, issuerUuid != null ? issuerUuid.toString() : "CONSOLE");
            ps.setString(2, issuerName);
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
    public Sanction getActiveSanction(UUID playerUuid, String playerName, SanctionType type) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM sanctions WHERE (player_uuid = ? OR player_name = ?) AND type = ? AND active = 1")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, type.name());
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
    public List<Sanction> getActiveSanctions(Collection<UUID> playerUuids, SanctionType type) {
        List<Sanction> result = new ArrayList<>();
        if (playerUuids == null || playerUuids.isEmpty()) return result;
        String placeholders = playerUuids.stream().map(u -> "?").collect(Collectors.joining(","));
        String sql = "SELECT * FROM sanctions WHERE player_uuid IN (" + placeholders + ") AND type = ? AND active = 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            int i = 1;
            for (UUID uuid : playerUuids) ps.setString(i++, uuid.toString());
            ps.setString(i, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sanction s = mapResultSetToSanction(rs);
                    if (!s.isExpired()) result.add(s);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public void savePlayerData(PlayerData data) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO player_data (uuid, last_name, last_ip, last_seen, first_seen, session_count, in_staff_mode) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getLastName());
            ps.setString(3, data.getLastIp());
            ps.setLong(4, data.getLastSeen());
            ps.setLong(5, data.getFirstSeen());
            ps.setInt(6, data.getSessionCount());
            ps.setBoolean(7, data.isInStaffMode());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToPlayerData(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    private PlayerData mapResultSetToPlayerData(ResultSet rs) throws SQLException {
        return new PlayerData(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("last_name"),
                rs.getString("last_ip"),
                rs.getLong("last_seen"),
                rs.getLong("first_seen"),
                rs.getInt("session_count"),
                rs.getBoolean("in_staff_mode")
        );
    }

    @Override
    public List<PlayerData> getAlts(String ip) {
        List<PlayerData> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_data WHERE last_ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapResultSetToPlayerData(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public int getLegitimateAccountCount(String ip) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT COUNT(*) FROM player_data WHERE last_ip = ? AND session_count > 5")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public void logAltSession(UUID uuid, String ip, String subnet, long connectedAt, int scoreAtLogin, boolean vpnDetected, String flags) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT INTO alt_sessions (uuid, ip, subnet, connected_at, score_at_login, vpn_detected, flags) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ps.setString(3, subnet);
            ps.setLong(4, connectedAt);
            ps.setInt(5, scoreAtLogin);
            ps.setBoolean(6, vpnDetected);
            ps.setString(7, flags);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateIPReputation(String ip, String subnet, int legitimateAccounts, int bannedAccounts, long lastUpdated, boolean natSuspected) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO ip_reputation (ip, subnet, legitimate_accounts, banned_accounts, last_updated, nat_suspected) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, ip);
            ps.setString(2, subnet);
            ps.setInt(3, legitimateAccounts);
            ps.setInt(4, bannedAccounts);
            ps.setLong(5, lastUpdated);
            ps.setBoolean(6, natSuspected);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public IPReputation getIPReputation(String ip) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM ip_reputation WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new IPReputation(
                            rs.getString("ip"),
                            rs.getString("subnet"),
                            rs.getInt("legitimate_accounts"),
                            rs.getInt("banned_accounts"),
                            rs.getLong("last_updated"),
                            rs.getBoolean("nat_suspected")
                    );
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void saveIPInfo(String ip, String countryCode, String countryName, String isp, boolean isProxy, long lastUpdate) {
        try (PreparedStatement ps = getConnection().prepareStatement("INSERT OR REPLACE INTO antivpn_cache (ip, country_code, country_name, isp, is_proxy, last_update) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, ip);
            ps.setString(2, countryCode);
            ps.setString(3, countryName);
            ps.setString(4, isp);
            ps.setBoolean(5, isProxy);
            ps.setLong(6, lastUpdate);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public fr.lampalon.lifemod.common.antivpn.data.IPInfo getIPInfo(String ip) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM antivpn_cache WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new fr.lampalon.lifemod.common.antivpn.data.IPInfo(rs.getString("ip"), rs.getString("country_code"), rs.getString("country_name"), rs.getString("isp"), rs.getBoolean("is_proxy"), rs.getLong("last_update"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void deleteExpiredIPInfo(long threshold) {
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM antivpn_cache WHERE last_update < ?")) {
            ps.setLong(1, threshold);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Report report = new Report(UUID.fromString(rs.getString("uuid")), UUID.fromString(rs.getString("reporter_uuid")), UUID.fromString(rs.getString("target_uuid")), rs.getString("reason"), rs.getString("server_name"), ReportStatus.valueOf(rs.getString("status")), rs.getString("assigned_to") != null ? UUID.fromString(rs.getString("assigned_to")) : null, rs.getLong("created_at"), rs.getLong("updated_at"), rs.getString("last_updated_by") != null ? UUID.fromString(rs.getString("last_updated_by")) : null, rs.getLong("closed_at"), rs.getString("close_reason"));
        report.setLocation(rs.getString("location_world"), rs.getDouble("location_x"), rs.getDouble("location_y"), rs.getDouble("location_z"));
        return report;
    }

    private Sanction mapResultSetToSanction(ResultSet rs) throws SQLException {
        Sanction s = new Sanction(
                UUID.fromString(rs.getString("uuid")),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("player_name"),
                rs.getString("issuer_uuid") != null ? UUID.fromString(rs.getString("issuer_uuid")) : null,
                rs.getString("issuer_name"),
                rs.getString("server_name"),
                rs.getString("category"),
                SanctionType.valueOf(rs.getString("type")),
                rs.getString("reason"),
                rs.getLong("created_at"),
                rs.getLong("duration"),
                rs.getBoolean("silent"),
                rs.getBoolean("active")
        );
        s.setEvidence(rs.getString("evidence"));
        String rb = rs.getString("removed_by_uuid");
        if (rb != null) s.revoke(UUID.fromString(rb), rs.getString("removed_by_name"), rs.getString("remove_reason"));
        return s;
    }
}
