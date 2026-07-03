package fr.lampalon.lifemod.common.database.type;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.AbstractDatabaseProvider;
import fr.lampalon.lifemod.common.model.*;
import fr.lampalon.lifemod.common.service.IConfigurationService;

import java.io.File;
import java.sql.*;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SQLiteManager extends AbstractDatabaseProvider {

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
    public void setupDatabase() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_inventories (uuid TEXT, server_name TEXT, inventory_data TEXT NOT NULL, saved_at INTEGER, PRIMARY KEY (uuid, server_name));");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_coords (uuid TEXT PRIMARY KEY, world TEXT, x REAL, y REAL, z REAL, yaw REAL, pitch REAL, saved_at INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sanctions (uuid TEXT PRIMARY KEY, player_uuid TEXT, player_name TEXT, issuer_uuid TEXT, issuer_name TEXT, server_name TEXT, category TEXT, type TEXT, reason TEXT, created_at INTEGER, duration INTEGER, silent BOOLEAN, active BOOLEAN, evidence TEXT, removed_by_uuid TEXT, removed_by_name TEXT, remove_reason TEXT, removed_at INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid TEXT PRIMARY KEY, last_name TEXT, last_ip TEXT, last_seen INTEGER, first_seen INTEGER, session_count INTEGER DEFAULT 0, in_staff_mode BOOLEAN DEFAULT 0);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS antivpn_cache (ip TEXT PRIMARY KEY, country_code TEXT, country_name TEXT, isp TEXT, is_proxy BOOLEAN, last_update INTEGER);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS alt_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid TEXT NOT NULL, ip TEXT NOT NULL, subnet TEXT NOT NULL, connected_at INTEGER NOT NULL, score_at_login INTEGER DEFAULT 0, vpn_detected BOOLEAN DEFAULT 0, flags TEXT);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ip_reputation (ip TEXT PRIMARY KEY, subnet TEXT NOT NULL, legitimate_accounts INTEGER DEFAULT 0, banned_accounts INTEGER DEFAULT 0, last_updated INTEGER, nat_suspected BOOLEAN DEFAULT 0);");

            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN in_staff_mode BOOLEAN DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN first_seen INTEGER;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_data ADD COLUMN session_count INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE player_inventories ADD COLUMN server_name TEXT;"); } catch (SQLException ignored) {}

            createLogTableIfNeeded();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
    public void saveSanction(Sanction sanction) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT OR REPLACE INTO sanctions (uuid, player_uuid, player_name, issuer_uuid, issuer_name, server_name, category, type, reason, created_at, duration, silent, active, evidence, removed_by_uuid, removed_by_name, remove_reason, removed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
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

    // --- Logs (monthly table rotation) ---

    private void createLogTableIfNeeded() {
        String table = logTableName();
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + table + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type INTEGER NOT NULL, " +
                "player_uuid TEXT NOT NULL, " +
                "player_name TEXT, " +
                "target_uuid TEXT, " +
                "target_name TEXT, " +
                "action_data TEXT, " +
                "world TEXT, " +
                "x INTEGER, y INTEGER, z INTEGER, " +
                "server_name TEXT, " +
                "created_at INTEGER NOT NULL" +
                ")");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_" + table + "_type ON " + table + "(type, created_at)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_" + table + "_player ON " + table + "(player_uuid, created_at)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_" + table + "_created ON " + table + "(created_at)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String logTableName() {
        YearMonth ym = YearMonth.now();
        return "action_logs_" + ym.getYear() + "_" + String.format("%02d", ym.getMonthValue());
    }

    private Set<String> getExistingLogTables() {
        Set<String> tables = new LinkedHashSet<>();
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'action_logs_%' ORDER BY name")) {
            while (rs.next()) tables.add(rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        return tables;
    }

    private List<String> logTableNamesForQuery(LogQuery query) {
        List<String> candidates = new ArrayList<>();
        Set<String> existing = getExistingLogTables();
        long from = query.getFromTime();
        long to = query.getToTime() == Long.MAX_VALUE ? System.currentTimeMillis() : query.getToTime();
        YearMonth fromMonth = YearMonth.from(Instant.ofEpochMilli(from).atZone(ZoneOffset.UTC));
        YearMonth toMonth = YearMonth.from(Instant.ofEpochMilli(to).atZone(ZoneOffset.UTC));
        YearMonth ym = fromMonth;
        while (!ym.isAfter(toMonth)) {
            String name = "action_logs_" + ym.getYear() + "_" + String.format("%02d", ym.getMonthValue());
            if (existing.contains(name)) candidates.add(name);
            ym = ym.plusMonths(1);
        }
        if (candidates.isEmpty()) candidates.add(logTableName());
        return candidates;
    }

    @Override
    public void saveLogBatch(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        synchronized (lock) {
            createLogTableIfNeeded();
            String table = logTableName();
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO " + table + " (type, player_uuid, player_name, target_uuid, target_name, action_data, world, x, y, z, server_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (LogEntry e : entries) {
                    setLogParameters(ps, e);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<LogEntry> queryLogs(LogQuery query) {
        List<LogEntry> results = new ArrayList<>();
        List<String> tables = logTableNamesForQuery(query);
        if (tables.isEmpty()) return results;

        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            if (i > 0) sql.append(" UNION ALL ");
            sql.append("SELECT * FROM ").append(tables.get(i)).append(" WHERE 1=1");
            if (query.getType() != null) sql.append(" AND type = ").append(query.getType().ordinal());
            if (query.getPlayerUuid() != null) sql.append(" AND player_uuid = '").append(query.getPlayerUuid()).append("'");
            if (query.getTargetUuid() != null) sql.append(" AND target_uuid = '").append(query.getTargetUuid()).append("'");
            if (query.getWorld() != null) sql.append(" AND world = '").append(query.getWorld().replace("'", "''")).append("'");
            if (query.getServerName() != null) sql.append(" AND server_name = '").append(query.getServerName().replace("'", "''")).append("'");
            sql.append(" AND created_at >= ").append(query.getFromTime()).append(" AND created_at <= ").append(query.getToTime());
        }
        sql.append(" ORDER BY created_at DESC LIMIT ").append(query.getLimit()).append(" OFFSET ").append(query.getOffset());

        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) results.add(mapLogEntry(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public long countLogs(LogQuery query) {
        List<String> tables = logTableNamesForQuery(query);
        if (tables.isEmpty()) return 0;

        StringBuilder sql = new StringBuilder("SELECT SUM(cnt) FROM (");
        for (int i = 0; i < tables.size(); i++) {
            if (i > 0) sql.append(" UNION ALL ");
            sql.append("SELECT COUNT(*) AS cnt FROM ").append(tables.get(i)).append(" WHERE 1=1");
            if (query.getType() != null) sql.append(" AND type = ").append(query.getType().ordinal());
            if (query.getPlayerUuid() != null) sql.append(" AND player_uuid = '").append(query.getPlayerUuid()).append("'");
            if (query.getTargetUuid() != null) sql.append(" AND target_uuid = '").append(query.getTargetUuid()).append("'");
            if (query.getWorld() != null) sql.append(" AND world = '").append(query.getWorld().replace("'", "''")).append("'");
            if (query.getServerName() != null) sql.append(" AND server_name = '").append(query.getServerName().replace("'", "''")).append("'");
            sql.append(" AND created_at >= ").append(query.getFromTime()).append(" AND created_at <= ").append(query.getToTime());
        }
        sql.append(")");

        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql.toString())) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void purgeLogs(Map<Integer, Long> retentionMsPerType, long defaultRetentionMs) {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            Set<String> tables = getExistingLogTables();
            String currentTable = logTableName();
            for (String table : tables) {
                if (table.equals(currentTable)) continue;
                String suffix = table.substring("action_logs_".length());
                String[] parts = suffix.split("_");
                if (parts.length != 2) continue;
                try {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    YearMonth ym = YearMonth.of(year, month);
                    long tableEndTime = ym.plusMonths(1).atDay(1)
                        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                    if (tableEndTime <= now - defaultRetentionMs) {
                        try (Statement stmt = getConnection().createStatement()) {
                            stmt.executeUpdate("DROP TABLE IF EXISTS " + table);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
