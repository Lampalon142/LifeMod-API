package fr.lampalon.lifemod.common.database;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.database.type.MySQLManager;
import fr.lampalon.lifemod.common.database.type.SQLiteManager;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private DatabaseProvider databaseProvider;

    public DatabaseManager() {
    }

    public void setupDatabase() {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        String type = config.getString("database.type", "sqlite").toLowerCase();

        try {
            switch (type) {
                case "mariadb":
                case "mysql":
                    databaseProvider = new MySQLManager();
                    break;
                case "sqlite":
                default:
                    databaseProvider = new SQLiteManager();
                    break;
            }

            databaseProvider.setupDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (databaseProvider == null) {
            throw new SQLException("Database provider not initialized!");
        }
        return databaseProvider.getConnection();
    }

    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public void closeConnection() {
        try {
            if (databaseProvider != null) {
                databaseProvider.closeConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
