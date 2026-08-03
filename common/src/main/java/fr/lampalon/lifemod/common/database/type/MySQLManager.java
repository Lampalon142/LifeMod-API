package fr.lampalon.lifemod.common.database.type;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.AbstractDatabaseProvider;
import fr.lampalon.lifemod.common.model.*;
import fr.lampalon.lifemod.common.service.IConfigurationService;

import java.sql.*;
import java.util.*;

public class MySQLManager extends AbstractDatabaseProvider {

    private HikariDataSource dataSource;

    @Override
    public void setupDatabase() {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.name", "lifemod");
        String user = config.getString("database.user", "root");
        String password = config.getString("database.password", "");
        int poolsize = config.getInt("database.poolsize", 10);

        HikariConfig hikariConfig = new HikariConfig();
        boolean useSSL = config.getBoolean("database.use-ssl", false);
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&serverTimezone=UTC");
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolsize);
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setLeakDetectionThreshold(10000);
        hikariConfig.setPoolName("LifeMod-MySQL");
        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_inventories (uuid VARCHAR(36), server_name VARCHAR(64), inventory_data LONGBLOB NOT NULL, saved_at BIGINT, PRIMARY KEY (uuid, server_name));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_coords (uuid VARCHAR(36) PRIMARY KEY, world VARCHAR(64), x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT, saved_at BIGINT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sanctions (uuid VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36), player_name VARCHAR(32), issuer_uuid VARCHAR(36), issuer_name VARCHAR(32), server_name VARCHAR(64), category VARCHAR(32), type VARCHAR(16), reason TEXT, created_at BIGINT, duration BIGINT, silent BOOLEAN, active BOOLEAN, evidence TEXT, removed_by_uuid VARCHAR(36), removed_by_name VARCHAR(32), remove_reason TEXT, removed_at BIGINT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid VARCHAR(36) PRIMARY KEY, last_name VARCHAR(32), last_ip VARCHAR(45), last_seen BIGINT, first_seen BIGINT, session_count INT DEFAULT 0, in_staff_mode BOOLEAN DEFAULT FALSE);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS antivpn_cache (ip VARCHAR(45) PRIMARY KEY, country_code VARCHAR(10), country_name VARCHAR(64), isp TEXT, is_proxy BOOLEAN, last_update BIGINT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS alt_sessions (id INT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) NOT NULL, ip VARCHAR(45) NOT NULL, subnet VARCHAR(12) NOT NULL, connected_at BIGINT NOT NULL, score_at_login INT DEFAULT 0, vpn_detected BOOLEAN DEFAULT FALSE, flags TEXT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ip_reputation (ip VARCHAR(45) PRIMARY KEY, subnet VARCHAR(12) NOT NULL, legitimate_accounts INT DEFAULT 0, banned_accounts INT DEFAULT 0, last_updated BIGINT, nat_suspected BOOLEAN DEFAULT FALSE);");

            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN in_staff_mode BOOLEAN DEFAULT FALSE;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN first_seen BIGINT;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN session_count INT DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE sanctions ADD COLUMN player_name VARCHAR(32) AFTER player_uuid;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_inventories ADD COLUMN server_name VARCHAR(64) AFTER uuid;"); } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE player_inventories DROP PRIMARY KEY;");
                stmt.executeUpdate("ALTER TABLE player_inventories ADD PRIMARY KEY (uuid, server_name);");
            } catch (SQLException ignored) {}

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reports (id INT AUTO_INCREMENT PRIMARY KEY, reporter_uuid VARCHAR(36) NOT NULL, reporter_name VARCHAR(32), target_uuid VARCHAR(36) NOT NULL, target_name VARCHAR(32), reason TEXT NOT NULL, server_name VARCHAR(64), location_world VARCHAR(64), location_x DOUBLE, location_y DOUBLE, location_z DOUBLE, status VARCHAR(16) DEFAULT 'OPEN', assigned_to VARCHAR(36), created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, closed_at BIGINT DEFAULT 0, replay_id VARCHAR(36));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS report_evidence (id INT AUTO_INCREMENT PRIMARY KEY, report_id INT NOT NULL, type VARCHAR(16) NOT NULL, data TEXT NOT NULL, author_uuid VARCHAR(36), author_name VARCHAR(32), created_at BIGINT NOT NULL, FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE);");

            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN reporter_name VARCHAR(32);"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN target_name VARCHAR(32);"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN server_name VARCHAR(64);"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN location_world VARCHAR(64);"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN location_x DOUBLE;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN location_y DOUBLE;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN location_z DOUBLE;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN assigned_to VARCHAR(36);"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN closed_at BIGINT DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE reports ADD COLUMN replay_id VARCHAR(36);"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("UPDATE reports SET status = UPPER(status);"); } catch (SQLException ignored) {}
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_replays (session_name VARCHAR(64) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, entity_id INT, player_name VARCHAR(32), world_name VARCHAR(64), start_x DOUBLE, start_y DOUBLE, start_z DOUBLE, start_yaw FLOAT, start_pitch FLOAT, duration_ms BIGINT, frame_count INT, data LONGBLOB NOT NULL, created_at BIGINT NOT NULL, is_report BOOLEAN DEFAULT FALSE);");
            try { stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_replays_created ON player_replays(created_at);"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_replays_report ON player_replays(is_report);"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public boolean supportsAsyncWrites() {
        return true;
    }

    @Override
    public void closeConnection() {
        if (dataSource != null) dataSource.close();
    }

    @Override
    public int saveReport(Report report) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO reports (reporter_uuid, reporter_name, target_uuid, target_name, reason, server_name, location_world, location_x, location_y, location_z, status, assigned_to, created_at, updated_at, closed_at, replay_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, report.getReporterUuid().toString());
            ps.setString(2, report.getReporterName());
            ps.setString(3, report.getTargetUuid().toString());
            ps.setString(4, report.getTargetName());
            ps.setString(5, report.getReason());
            ps.setString(6, report.getServerName());
            ps.setString(7, report.getLocationWorld());
            ps.setDouble(8, report.getLocationX());
            ps.setDouble(9, report.getLocationY());
            ps.setDouble(10, report.getLocationZ());
            ps.setString(11, report.getStatus().name());
            ps.setString(12, report.getAssignedTo() != null ? report.getAssignedTo().toString() : null);
            ps.setLong(13, report.getCreatedAt());
            ps.setLong(14, report.getUpdatedAt());
            ps.setLong(15, report.getClosedAt());
            ps.setString(16, report.getReplayId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    @Override
    public void addEvidence(ReportEvidence evidence) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO report_evidence (report_id, type, data, author_uuid, author_name, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, evidence.getReportId());
            ps.setString(2, evidence.getType());
            ps.setString(3, evidence.getData());
            ps.setString(4, evidence.getAuthorUuid() != null ? evidence.getAuthorUuid().toString() : null);
            ps.setString(5, evidence.getAuthorName());
            ps.setLong(6, evidence.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void saveRawInventory(UUID uuid, String serverName, byte[] data) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("REPLACE INTO player_inventories (uuid, server_name, inventory_data, saved_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, serverName);
            ps.setBytes(3, data);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void saveCoords(UUID uuid, String world, double x, double y, double z, float yaw, float pitch) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("REPLACE INTO player_coords (uuid, world, x, y, z, yaw, pitch, saved_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
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
    public void saveSanction(Sanction sanction) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sanctions (uuid, player_uuid, player_name, issuer_uuid, issuer_name, server_name, category, type, reason, created_at, duration, silent, active, evidence, removed_by_uuid, removed_by_name, remove_reason, removed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE active=VALUES(active), evidence=VALUES(evidence), removed_by_uuid=VALUES(removed_by_uuid), removed_by_name=VALUES(removed_by_name), remove_reason=VALUES(remove_reason), removed_at=VALUES(removed_at)")) {
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
    public void savePlayerData(PlayerData data) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO player_data (uuid, last_name, last_ip, last_seen, first_seen, session_count, in_staff_mode) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_name=VALUES(last_name), last_ip=VALUES(last_ip), last_seen=VALUES(last_seen), first_seen=VALUES(first_seen), session_count=VALUES(session_count), in_staff_mode=VALUES(in_staff_mode)")) {
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
    public void updateIPReputation(String ip, String subnet, int legitimateAccounts, int bannedAccounts, long lastUpdated, boolean natSuspected) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ip_reputation (ip, subnet, legitimate_accounts, banned_accounts, last_updated, nat_suspected) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE legitimate_accounts=VALUES(legitimate_accounts), banned_accounts=VALUES(banned_accounts), last_updated=VALUES(last_updated), nat_suspected=VALUES(nat_suspected)")) {
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
    public void saveIPInfo(String ip, String countryCode, String countryName, String isp, boolean isProxy, long lastUpdate) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO antivpn_cache (ip, country_code, country_name, isp, is_proxy, last_update) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE country_code=VALUES(country_code), country_name=VALUES(country_name), isp=VALUES(isp), is_proxy=VALUES(is_proxy), last_update=VALUES(last_update)")) {
            ps.setString(1, ip);
            ps.setString(2, countryCode);
            ps.setString(3, countryName);
            ps.setString(4, isp);
            ps.setBoolean(5, isProxy);
            ps.setLong(6, lastUpdate);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
