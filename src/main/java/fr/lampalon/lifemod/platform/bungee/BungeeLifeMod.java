package fr.lampalon.lifemod.platform.bungee;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.messaging.RedisMessagingService;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.SanctionService;
import fr.lampalon.lifemod.common.database.DatabaseManager;
import fr.lampalon.lifemod.platform.bungee.adapter.BungeeConfigurationService;
import fr.lampalon.lifemod.platform.bungee.adapter.BungeeLangService;
import fr.lampalon.lifemod.platform.bungee.listeners.BungeeChatListener;
import fr.lampalon.lifemod.platform.bungee.listeners.BungeeConnectionListener;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BungeeLifeMod extends Plugin {

    private static BungeeLifeMod instance;
    private Configuration config;
    private Configuration lang;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;
        loadConfigs();

        ILifePlatform platform = new BungeePlatform(this);
        ServiceRegistry.register(ILifePlatform.class, platform);
        ServiceRegistry.register(IConfigurationService.class, new BungeeConfigurationService(config));
        ServiceRegistry.register(ILangService.class, new BungeeLangService(lang));

        if (config.getBoolean("redis.enabled", false)) {
            String host = config.getString("redis.host", "localhost");
            int port = config.getInt("redis.port", 6379);
            String password = config.getString("redis.password", "");
            IMessagingService redis = new RedisMessagingService(host, port, password);
            ServiceRegistry.register(IMessagingService.class, redis);
        }

        this.databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();
        
        ServiceRegistry.register(ISanctionService.class, new SanctionService(databaseManager.getDatabaseProvider()));

        getProxy().getPluginManager().registerListener(this, new BungeeConnectionListener());
        getProxy().getPluginManager().registerListener(this, new BungeeChatListener());

        long elapsed = System.currentTimeMillis() - start;

        // Detailed Startup Message
        ProxyServer.getInstance().getLogger().info("§8§m----------------------------------------");
        ProxyServer.getInstance().getLogger().info("§6§lLifeMod §bBungee §7- §aSuccessfully Enabled");
        ProxyServer.getInstance().getLogger().info(" ");
        ProxyServer.getInstance().getLogger().info("§e• §fVersion: §b" + getDescription().getVersion());
        ProxyServer.getInstance().getLogger().info("§e• §fPlatform: §aBungeeCord §7(" + ProxyServer.getInstance().getName() + " " + ProxyServer.getInstance().getVersion() + ")");
        ProxyServer.getInstance().getLogger().info("§e• §fDatabase: §a" + config.getString("database.type", "mysql").toUpperCase());
        ProxyServer.getInstance().getLogger().info("§e• §fRedis Sync: " + (config.getBoolean("redis.enabled", false) ? "§aEnabled" : "§cDisabled"));
        ProxyServer.getInstance().getLogger().info("§e• §fStartup Time: §e" + elapsed + "ms");
        ProxyServer.getInstance().getLogger().info(" ");
        ProxyServer.getInstance().getLogger().info("§8§m----------------------------------------");
    }

    private void loadConfigs() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        File configFile = new File(getDataFolder(), "config.yml");
        File langFile = new File(getDataFolder(), "lang.yml");

        try {
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("bungee-config.yml")) {
                    java.nio.file.Files.copy(in, configFile.toPath());
                }
            }
            if (!langFile.exists()) {
                try (InputStream in = getResourceAsStream("bungee-lang.yml")) {
                    java.nio.file.Files.copy(in, langFile.toPath());
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            lang = ConfigurationProvider.getProvider(YamlConfiguration.class).load(langFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BungeeLifeMod getInstance() {
        return instance;
    }
}
