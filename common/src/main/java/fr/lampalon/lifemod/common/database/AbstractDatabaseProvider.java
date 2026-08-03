package fr.lampalon.lifemod.common.database;

import fr.lampalon.lifemod.common.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public abstract class AbstractDatabaseProvider implements DatabaseProvider {

    @Override
    public abstract Connection getConnection() throws SQLException;

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
    public int saveReport(Report report) {
        // Per-subclass impl via abstract method
        throw new UnsupportedOperationException("Override in subclass");
    }

    @Override
    public Report getReportById(int id) {
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM reports WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapReport(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<Report> getAllReports(int limit, int offset) {
        List<Report> reports = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM reports ORDER BY CASE status WHEN 'OPEN' THEN 0 WHEN 'ASSIGNED' THEN 1 WHEN 'CLOSED' THEN 2 WHEN 'REJECTED' THEN 3 ELSE 4 END, created_at DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reports.add(mapReport(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    @Override
    public List<Report> getReportsByStatus(String status, int limit, int offset) {
        List<Report> reports = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM reports WHERE status = ? ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
            ps.setString(1, status);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reports.add(mapReport(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return reports;
    }

    @Override
    public void updateReportStatus(int id, String status, UUID assignedTo) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE reports SET status = ?, assigned_to = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setString(2, assignedTo != null ? assignedTo.toString() : null);
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void addEvidence(ReportEvidence evidence) {
        // Per-subclass impl
        throw new UnsupportedOperationException("Override in subclass");
    }

    @Override
    public List<ReportEvidence> getEvidence(int reportId) {
        List<ReportEvidence> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement("SELECT * FROM report_evidence WHERE report_id = ? ORDER BY created_at ASC")) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ReportEvidence(
                            rs.getInt("id"),
                            rs.getInt("report_id"),
                            rs.getString("type"),
                            rs.getString("data"),
                            rs.getString("author_uuid") != null ? UUID.fromString(rs.getString("author_uuid")) : null,
                            rs.getString("author_name"),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    protected Report mapReport(ResultSet rs) throws SQLException {
        Report r = new Report(
                rs.getInt("id"),
                UUID.fromString(rs.getString("reporter_uuid")),
                rs.getString("reporter_name"),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                rs.getString("reason"),
                rs.getString("server_name"),
                ReportStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getString("assigned_to") != null ? UUID.fromString(rs.getString("assigned_to")) : null,
                rs.getLong("created_at"),
                rs.getLong("updated_at"),
                rs.getLong("closed_at"),
                rs.getString("replay_id"));
        r.setLocation(rs.getString("location_world"), rs.getDouble("location_x"),
                rs.getDouble("location_y"), rs.getDouble("location_z"));
        return r;
    }

    @Override
    public void setReportReplayId(int reportId, String replayId) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE reports SET replay_id = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, replayId);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, reportId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Replays ---

    @Override
    public void saveReplay(String sessionName, UUID playerUuid, int entityId, String playerName, String worldName,
                           double startX, double startY, double startZ, float startYaw, float startPitch,
                           long durationMs, int frameCount, byte[] data) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO player_replays (session_name, player_uuid, entity_id, player_name, world_name, start_x, start_y, start_z, start_yaw, start_pitch, duration_ms, frame_count, data, created_at, is_report) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)")) {
            ps.setString(1, sessionName);
            ps.setString(2, playerUuid.toString());
            ps.setInt(3, entityId);
            ps.setString(4, playerName);
            ps.setString(5, worldName);
            ps.setDouble(6, startX);
            ps.setDouble(7, startY);
            ps.setDouble(8, startZ);
            ps.setFloat(9, startYaw);
            ps.setFloat(10, startPitch);
            ps.setLong(11, durationMs);
            ps.setInt(12, frameCount);
            ps.setBytes(13, data);
            ps.setLong(14, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<DatabaseProvider.ReplayMeta> listReplays(int limit, int offset) {
        List<DatabaseProvider.ReplayMeta> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM player_replays ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapReplayMeta(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public DatabaseProvider.ReplayMeta getReplayMeta(String sessionName) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM player_replays WHERE session_name = ?")) {
            ps.setString(1, sessionName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapReplayMeta(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public byte[] getReplayData(String sessionName) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT data FROM player_replays WHERE session_name = ?")) {
            ps.setString(1, sessionName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBytes("data");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void deleteReplay(String sessionName) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM player_replays WHERE session_name = ?")) {
            ps.setString(1, sessionName);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void deleteExpiredReplays(long thresholdMs) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM player_replays WHERE is_report = 0 AND created_at < ?")) {
            ps.setLong(1, thresholdMs);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void markReplayAsReport(String sessionName) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE player_replays SET is_report = 1 WHERE session_name = ?")) {
            ps.setString(1, sessionName);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private DatabaseProvider.ReplayMeta mapReplayMeta(ResultSet rs) throws SQLException {
        return new DatabaseProvider.ReplayMeta(
                rs.getString("session_name"),
                UUID.fromString(rs.getString("player_uuid")),
                rs.getInt("entity_id"),
                rs.getString("player_name"),
                rs.getString("world_name"),
                rs.getDouble("start_x"), rs.getDouble("start_y"), rs.getDouble("start_z"),
                rs.getFloat("start_yaw"), rs.getFloat("start_pitch"),
                rs.getLong("duration_ms"),
                rs.getInt("frame_count"),
                rs.getLong("created_at"),
                rs.getBoolean("is_report"));
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
        ps.setString(2, e.getPlayerUuid() != null ? e.getPlayerUuid().toString() : "00000000-0000-0000-0000-000000000000");
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
