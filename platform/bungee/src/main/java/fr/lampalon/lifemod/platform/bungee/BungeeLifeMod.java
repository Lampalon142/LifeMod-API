package fr.lampalon.lifemod.platform.bungee;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.messaging.RedisMessagingService;
import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.analytics.PostHogService;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.SanctionService;
import fr.lampalon.lifemod.common.database.DatabaseManager;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bungee.adapter.BungeeConfigurationService;
import fr.lampalon.lifemod.platform.bungee.adapter.BungeeLangService;
import fr.lampalon.lifemod.platform.bungee.listeners.BungeeAntiAltListener;
import fr.lampalon.lifemod.platform.bungee.listeners.BungeeAntiVPNListener;
import fr.lampalon.lifemod.platform.bungee.listeners.BungeeChatListener;
import fr.lampalon.lifemod.platform.bungee.listeners.BungeeConnectionListener;
import fr.lampalon.lifemod.platform.bungee.managers.BungeeReactionManager;
import fr.lampalon.lifemod.platform.bungee.managers.antialt.BungeeAntiAltManager;
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
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BungeeLifeMod extends Plugin {

    private static BungeeLifeMod instance;
    private Configuration config;
    private Configuration lang;
    private DatabaseManager databaseManager;
    private BungeeAntiAltManager antiAltManager;
    private BungeeReactionManager reactionManager;
    private fr.lampalon.lifemod.common.antivpn.AntiVPNService antiVPNService;
    private long startupTime;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        this.startupTime = start;
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

            ILangService lang = ServiceRegistry.get(ILangService.class);
            redis.subscribe("lifemod:sanctions", message -> {
                try {
                    int pipe = message.indexOf('|');
                    String action = pipe > 0 ? message.substring(0, pipe) : "";
                    String data = pipe > 0 ? message.substring(pipe + 1) : "";

                    if ("ADD".equals(action)) {
                        String[] parts = data.split("\\|", -1);
                        if (parts.length < 8) return;
                        SanctionType type = SanctionType.valueOf(parts[0]);
                        UUID playerUuid = UUID.fromString(parts[1]);
                        String issuerName = parts[2];
                        String reason = parts[3];
                        long duration = Long.parseLong(parts[4]);
                        boolean isSilent = Boolean.parseBoolean(parts[5]);
                        String origServer = parts[6];

                        String targetName = ProxyServer.getInstance().getPlayer(playerUuid) != null
                                ? ProxyServer.getInstance().getPlayer(playerUuid).getName()
                                : playerUuid.toString().substring(0, 8);

                        String path = "sanctions.broadcast." + type.name().toLowerCase() + (isSilent ? ".silent" : ".public");
                        String msg = lang.getMessage(path,
                                "%target%", targetName,
                                "%issuer%", issuerName,
                                "%reason%", reason,
                                "%time%", TimeUtil.formatTime(duration),
                                "%server%", origServer);

                        if (!msg.equals(path)) {
                            if (isSilent) {
                                platform.broadcast(msg, "lifemod.sanctions.see-silent");
                            } else {
                                platform.broadcast(msg, null);
                            }
                        }

                        if (type == SanctionType.BAN) {
                            ProxiedPlayer online = ProxyServer.getInstance().getPlayer(playerUuid);
                            if (online != null && online.isConnected()) {
                                String kickReason = lang.getMessage("sanctions.ban.login",
                                        "%reason%", reason,
                                        "%issuer%", issuerName,
                                        "%expiration%", duration == 0 ? lang.getMessage("sanctions.permanent") : TimeUtil.formatTime(duration),
                                        "%id%", playerUuid.toString().substring(0, 8),
                                        "%server%", origServer);
                                online.disconnect(new TextComponent(lang.formatMessage(kickReason)));
                            }
                        }
                    } else if ("REMOVE".equals(action)) {
                        String[] parts = data.split("\\|", -1);
                        if (parts.length < 5) return;
                        SanctionType type = SanctionType.valueOf(parts[0]);
                        UUID playerUuid = UUID.fromString(parts[1]);
                        String removedByName = parts[2];
                        String reason = parts[3];
                        boolean silent = Boolean.parseBoolean(parts[4]);

                        String targetName = ProxyServer.getInstance().getPlayer(playerUuid) != null
                                ? ProxyServer.getInstance().getPlayer(playerUuid).getName()
                                : playerUuid.toString().substring(0, 8);

                        String path = "sanctions.broadcast.un" + type.name().toLowerCase() + (silent ? ".silent" : ".public");
                        String msg = lang.getMessage(path,
                                "%target%", targetName,
                                "%issuer%", removedByName,
                                "%reason%", reason);

                        if (!msg.equals(path)) {
                            if (silent) {
                                platform.broadcast(msg, "lifemod.sanctions.see-silent");
                            } else {
                                platform.broadcast(msg, null);
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to process cross-server sanction: " + message);
                }
            });
        }

        this.databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();

        this.antiVPNService = new fr.lampalon.lifemod.common.antivpn.AntiVPNService(platform);
        ServiceRegistry.register(fr.lampalon.lifemod.common.antivpn.AntiVPNService.class, this.antiVPNService);
        getProxy().getPluginManager().registerListener(this, new BungeeAntiVPNListener(this, this.antiVPNService));

        this.antiAltManager = new BungeeAntiAltManager(this);
        this.reactionManager = new BungeeReactionManager(this);

        ServiceRegistry.register(ISanctionService.class, new SanctionService(databaseManager.getDatabaseProvider()));

        getProxy().getPluginManager().registerListener(this, new BungeeConnectionListener());
        getProxy().getPluginManager().registerListener(this, new BungeeChatListener());
        getProxy().getPluginManager().registerListener(this, new BungeeAntiAltListener(this));

        setupPostHog();

        long elapsed = System.currentTimeMillis() - start;

        getLogger().info("&8&m----------------------------------------");
        getLogger().info("&6&lLifeMod &bBungee &7- &aSuccessfully Enabled");
        getLogger().info(" ");
        getLogger().info("&e• &fVersion: &b" + getDescription().getVersion());
        getLogger().info("&e• &fPlatform: &aBungeeCord");
        getLogger().info("&e• &fInstance: &d" + ProxyServer.getInstance().getVersion());
        getLogger().info("&e• &fDatabase: &a" + config.getString("database.type", "mysql").toUpperCase());
        getLogger().info("&e• &fRedis Sync: " + (config.getBoolean("redis.enabled", false) ? "&aEnabled" : "&cDisabled"));
        getLogger().info("&e• &fStartup Time: &e" + elapsed + "ms");
        getLogger().info(" ");
        getLogger().info("&8&m----------------------------------------");
    }

    private void loadConfigs() {
        if (!getDataFolder().exists()) getDataFolder().mkdir();
        File configFile = new File(getDataFolder(), "config.yml");

        try {
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("bungee-config.yml")) {
                    java.nio.file.Files.copy(in, configFile.toPath());
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            String langName = config.getString("server.language", "en_US");
            loadLanguageConfig(langName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLanguageConfig(String langName) {
        File langFolder = new File(getDataFolder(), "languages");
        if (!langFolder.exists()) langFolder.mkdirs();

        File langFile = new File(langFolder, langName + ".yml");
        if (!langFile.exists()) {
            String resourcePath = "bungee_" + langName + ".yml";
            if (getResourceAsStream(resourcePath) != null) {
                try (InputStream in = getResourceAsStream(resourcePath)) {
                    java.nio.file.Files.copy(in, langFile.toPath());
                } catch (IOException e) { e.printStackTrace(); }
            } else {
                // Fallback
                getLogger().warning("Language '" + langName + "' not found. Falling back to en_US.");
                langFile = new File(langFolder, "en_US.yml");
                if (!langFile.exists()) {
                    try (InputStream in = getResourceAsStream("bungee_en_US.yml")) {
                        java.nio.file.Files.copy(in, langFile.toPath());
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }

        try {
            lang = ConfigurationProvider.getProvider(YamlConfiguration.class).load(langFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BungeeLifeMod getInstance() {
        return instance;
    }

    public Configuration getConfig() {
        return config;
    }

    public BungeeAntiAltManager getAntiAltManager() {
        return antiAltManager;
    }

    public BungeeReactionManager getReactionManager() {
        return reactionManager;
    }

    private void setupPostHog() {
        if (!config.getBoolean("modules.posthog.enabled", true)) return;
        String host = "https://eu.posthog.com";

        getLogger().info("PostHog: resolving API key...");
        String apiKey = PostHogService.resolveApiKey();
        String serverVersion = ProxyServer.getInstance().getVersion();
        String javaVersion = System.getProperty("java.version");
        String dbType = config.getString("database.type", "mysql");
        boolean redis = config.getBoolean("redis.enabled", false);

        getLogger().info("PostHog: creating service (host=" + host + ")...");
        try {
            PostHogService service = new PostHogService(apiKey, config.getString("server.name", "Proxy"), getDescription().getVersion(), "bungee", host, serverVersion, javaVersion, dbType, redis, -1, getDataFolder().getPath());
            ServiceRegistry.register(IPostHogService.class, service);
            getLogger().info("PostHog: registered in ServiceRegistry");

            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            Runtime runtime = Runtime.getRuntime();
            service.sendEnvironmentEvent(
                    os.getName(), os.getArch(), os.getVersion(),
                    runtime.availableProcessors(),
                    runtime.maxMemory() / 1048576,
                    runtime.totalMemory() / 1048576
            );

            Map<String, Boolean> modules = new HashMap<>();
            modules.put("antialt", config.getBoolean("modules.antialt.enabled", true));
            modules.put("auto_punish", config.getBoolean("modules.auto-punish.enabled", true));

            Map<String, Object> extra = new HashMap<>();
            extra.put("database_type", config.getString("database.type", "mysql"));
            extra.put("redis_enabled", config.getBoolean("redis.enabled", false));
            extra.put("antivpn_geo_mode", "NONE");
            extra.put("auto_punish_mode", config.getString("modules.auto-punish.mode", "GLOBAL"));

            service.sendConfigSnapshot(modules, extra);

            getLogger().info("PostHog analytics enabled");
        } catch (Exception e) {
            getLogger().warning("PostHog FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("plugin_version", getDescription().getVersion());
            props.put("platform", "bungee");
            props.put("uptime_seconds", (System.currentTimeMillis() - startupTime) / 1000);
            ph.capture("lifemod_shutdown", props);
            ph.shutdown();
        }
    }
}
