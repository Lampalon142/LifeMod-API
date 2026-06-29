package fr.lampalon.lifemod.common.database;

import fr.lampalon.lifemod.common.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class AbstractDatabaseProvider implements DatabaseProvider {

    @Override
    public abstract Connection getConnection() throws SQLException;

    protected Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Report report = new Report(
                UUID.fromString(rs.getString("uuid")),
                UUID.fromString(rs.getString("reporter_uuid")),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("reason"),
                rs.getString("server_name"),
                ReportStatus.valueOf(rs.getString("status")),
                rs.getString("assigned_to") != null ? UUID.fromString(rs.getString("assigned_to")) : null,
                rs.getLong("created_at"),
                rs.getLong("updated_at"),
                rs.getString("last_updated_by") != null ? UUID.fromString(rs.getString("last_updated_by")) : null,
                rs.getLong("closed_at"),
                rs.getString("close_reason"));
        report.setLocation(rs.getString("location_world"), rs.getDouble("location_x"),
                rs.getDouble("location_y"), rs.getDouble("location_z"));
        return report;
    }

    protected Sanction mapResultSetToSanction(ResultSet rs) throws SQLException {
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
                rs.getBoolean("active"));
        s.setEvidence(rs.getString("evidence"));
        String rb = rs.getString("removed_by_uuid");
        if (rb != null) s.revoke(UUID.fromString(rb), rs.getString("removed_by_name"), rs.getString("remove_reason"));
        return s;
    }

    protected PlayerData mapResultSetToPlayerData(ResultSet rs) throws SQLException {
        return new PlayerData(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("last_name"),
                rs.getString("last_ip"),
                rs.getLong("last_seen"),
                rs.getLong("first_seen"),
                rs.getInt("session_count"),
                rs.getBoolean("in_staff_mode"));
    }

    protected LogEntry mapLogEntry(ResultSet rs) throws SQLException {
        return LogEntry.builder()
                .id(rs.getLong("id"))
                .type(rs.getInt("type"))
                .playerUuid(UUID.fromString(rs.getString("player_uuid")))
                .playerName(rs.getString("player_name"))
                .targetUuid(rs.getString("target_uuid") != null ? UUID.fromString(rs.getString("target_uuid")) : null)
                .targetName(rs.getString("target_name"))
                .actionData(rs.getString("action_data"))
                .world(rs.getString("world"))
                .x(rs.getInt("x")).y(rs.getInt("y")).z(rs.getInt("z"))
                .serverName(rs.getString("server_name"))
                .createdAt(rs.getLong("created_at"))
                .build();
    }

    // --- Reports ---

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
                while (rs.next()) notes.add(new StaffNote(
                        UUID.fromString(rs.getString("note_id")),
                        UUID.fromString(rs.getString("author")),
                        rs.getLong("created_at"),
                        rs.getLong("updated_at"),
                        rs.getString("content")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return notes;
    }

    @Override
    public void addStaffNote(UUID reportId, StaffNote note) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO report_staff_notes (note_id, report_id, author, created_at, updated_at, content) VALUES (?, ?, ?, ?, ?, ?)")) {
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

    // --- Inventories ---

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

    // --- Coords ---

    @Override
    public StoredLocation getCoords(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM player_coords WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new StoredLocation(
                        rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // --- Sanctions ---

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
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE sanctions SET active = 0 WHERE active = 1 AND duration > 0 AND (created_at + duration) < ?")) {
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
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM sanctions WHERE issuer_uuid = ? OR issuer_name = ? ORDER BY created_at DESC")) {
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
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM sanctions WHERE (player_uuid = ? OR player_name = ?) AND type = ? AND active = 1")) {
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
        String placeholders = String.join(",", Collections.nCopies(playerUuids.size(), "?"));
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

    // --- Player Data ---

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
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM player_data WHERE last_ip = ? AND session_count > 5")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // --- Alt Sessions & Reputation ---

    @Override
    public void logAltSession(UUID uuid, String ip, String subnet, long connectedAt, int scoreAtLogin, boolean vpnDetected, String flags) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO alt_sessions (uuid, ip, subnet, connected_at, score_at_login, vpn_detected, flags) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
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
    public IPReputation getIPReputation(String ip) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM ip_reputation WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new IPReputation(
                            rs.getString("ip"), rs.getString("subnet"),
                            rs.getInt("legitimate_accounts"), rs.getInt("banned_accounts"),
                            rs.getLong("last_updated"), rs.getBoolean("nat_suspected"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // --- AntiVPN Cache ---

    @Override
    public fr.lampalon.lifemod.common.antivpn.data.IPInfo getIPInfo(String ip) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM antivpn_cache WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new fr.lampalon.lifemod.common.antivpn.data.IPInfo(
                        rs.getString("ip"), rs.getString("country_code"), rs.getString("country_name"),
                        rs.getString("isp"), rs.getBoolean("is_proxy"), rs.getLong("last_update"));
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

    // --- Logs (shared helper) ---

    protected void setLogParameters(PreparedStatement ps, LogEntry e) throws SQLException {
        ps.setInt(1, e.getType());
        ps.setString(2, e.getPlayerUuid().toString());
        ps.setString(3, e.getPlayerName());
        ps.setString(4, e.getTargetUuid() != null ? e.getTargetUuid().toString() : null);
        ps.setString(5, e.getTargetName());
        ps.setString(6, e.getActionData());
        ps.setString(7, e.getWorld());
        ps.setInt(8, e.getX());
        ps.setInt(9, e.getY());
        ps.setInt(10, e.getZ());
        ps.setString(11, e.getServerName());
        ps.setLong(12, e.getCreatedAt());
    }
}
