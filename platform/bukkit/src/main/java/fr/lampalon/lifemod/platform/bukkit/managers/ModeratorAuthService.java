package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.service.IPinService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.common.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class ModeratorAuthService implements IPinService {
    private final LifeMod plugin;
    private final DatabaseManager dbManager;
    private final java.util.Map<UUID, String> hashCache = new java.util.concurrent.ConcurrentHashMap<>();

    public ModeratorAuthService(LifeMod plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
        createTableIfNotExists();
    }

    public void invalidateHashCache(UUID uuid) {
        hashCache.remove(uuid);
    }

    @Override
    public void registerPin(UUID uuid, String pin) {
        // We need the name for registration. If not available, we try to get it from Bukkit.
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null) name = "UNKNOWN";
        String ip = "UNKNOWN";
        if (Bukkit.getPlayer(uuid) != null) {
            ip = Bukkit.getPlayer(uuid).getAddress().getAddress().getHostAddress();
        }
        registerModerator(uuid, name, pin, ip);
    }

    @Override
    public boolean verifyPin(UUID uuid, String enteredPin) {
        return checkPassword(uuid, enteredPin);
    }

    @Override
    public boolean resetPin(UUID uuid, String newPin) {
        changePassword(uuid, newPin);
        return true;
    }

    @Override
    public Optional<String> getPinStatus(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash FROM moderator_auth WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] getPinStatus error: " + e.getMessage());
        }
        return Optional.empty();
    }

    private void createTableIfNotExists() {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS moderator_auth (" +
                             "uuid VARCHAR(36) PRIMARY KEY," +
                             "name VARCHAR(32)," +
                             "password_hash VARCHAR(256) NOT NULL," +
                             "ip VARCHAR(64)," +
                             "last_update BIGINT," +
                             "last_auth_ip VARCHAR(64)," +
                             "last_auth_time BIGINT" +
                             ")"
             )) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[LifeMod] Unable to create moderator_auth table: " + e.getMessage());
        }
        // Migration for existing tables that lack the new columns
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("ALTER TABLE moderator_auth ADD COLUMN last_auth_ip VARCHAR(64)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().fine("[LifeMod] Migration skippable: " + e.getMessage());
        }
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("ALTER TABLE moderator_auth ADD COLUMN last_auth_time BIGINT")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().fine("[LifeMod] Migration skippable: " + e.getMessage());
        }
    }

    public boolean isRegistered(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM moderator_auth WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] isRegistered error: " + e.getMessage());
            return false;
        }
    }

    public void registerModerator(UUID uuid, String name, String password, String ip) {
        String hash = hash(password);
        hashCache.put(uuid, hash);
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "REPLACE INTO moderator_auth (uuid, name, password_hash, ip, last_update) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, hash);
            ps.setString(4, ip);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[LifeMod] registerModerator error: " + e.getMessage());
        }
    }

    public void resetModeratorPassword(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM moderator_auth WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[LifeMod] resetModeratorPassword error: " + e.getMessage());
        }
    }

    public String getStoredIp(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT ip FROM moderator_auth WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("ip");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] getStoredIp error: " + e.getMessage());
        }
        return null;
    }

    public void updateIp(UUID uuid, String ip) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE moderator_auth SET ip = ?, last_update = ? WHERE uuid = ?")) {
            ps.setString(1, ip);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[LifeMod] updateIp error: " + e.getMessage());
        }
    }

    public void saveSession(UUID uuid, String ip) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE moderator_auth SET last_auth_ip = ?, last_auth_time = ? WHERE uuid = ?")) {
            ps.setString(1, ip);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[LifeMod] saveSession error: " + e.getMessage());
        }
    }

    public long getLastAuthTime(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT last_auth_time FROM moderator_auth WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long val = rs.getLong("last_auth_time");
                    return rs.wasNull() ? 0 : val;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] getLastAuthTime error: " + e.getMessage());
        }
        return 0;
    }

    public String getLastAuthIp(UUID uuid) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT last_auth_ip FROM moderator_auth WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("last_auth_ip");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] getLastAuthIp error: " + e.getMessage());
        }
        return null;
    }

    public UUID getUUIDByName(String name) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid, name FROM moderator_auth WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String dbName = rs.getString("name");
                    if (!dbName.equals(name)) {
                        updateName(UUID.fromString(uuidStr), name);
                    }
                    return UUID.fromString(uuidStr);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] getUUIDByName error: " + e.getMessage());
        }
        return null;
    }

    public void updateName(UUID uuid, String newName) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE moderator_auth SET name = ? WHERE uuid = ?")) {
            ps.setString(1, newName);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] updateName error: " + e.getMessage());
        }
    }

    private String hash(String input) {
        return BCrypt.hashpw(input, BCrypt.gensalt(12));
    }

    public boolean checkPassword(UUID uuid, String password) {
        String hash = hashCache.computeIfAbsent(uuid, u -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash FROM moderator_auth WHERE uuid = ?")) {
                ps.setString(1, u.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("password_hash");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[LifeMod] checkPassword error: " + e.getMessage());
            }
            return null;
        });
        return hash != null && BCrypt.checkpw(password, hash);
    }

    public void checkPasswordAsync(UUID uuid, String password, java.util.function.Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean result = checkPassword(uuid, password);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    public void changePassword(UUID uuid, String newPassword) {
        String hash = hash(newPassword);
        hashCache.put(uuid, hash);
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE moderator_auth SET password_hash = ?, last_update = ? WHERE uuid = ?")) {
            ps.setString(1, hash);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[LifeMod] changePassword error: " + e.getMessage());
        }
    }

    public java.util.List<String> getAllRegisteredNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM moderator_auth");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[LifeMod] getAllRegisteredNames error: " + e.getMessage());
        }
        return names;
    }
}


