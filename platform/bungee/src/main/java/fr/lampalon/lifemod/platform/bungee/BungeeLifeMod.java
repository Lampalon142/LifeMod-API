package fr.lampalon.lifemod.platform.bungee;

import fr.lampalon.lifemod.common.antivpn.AntiVPNService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseManager;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.messaging.RedisMessagingService;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.SanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bungee.adapter.BungeeConfigurationService;
import fr.lampalon.lifemod.platform.bungee.adapter.BungeeLangService;
import fr.lampalon.lifemod.platform.bungee.commands.BungeeLifeModCommand;
import fr.lampalon.lifemod.platform.bungee.commands.BungeeStaffchatCommand;
import fr.lampalon.lifemod.platform.bungee.listeners.*;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeeLifeMod extends Plugin {

    private static BungeeLifeMod instance;
    private Configuration config;
    private Configuration lang;
    private DatabaseManager databaseManager;
    private BungeeAntiAltManager antiAltManager;
    private BungeeReactionManager reactionManager;
    private AntiVPNService antiVPNService;
    private final Set<UUID> staffChatToggled = ConcurrentHashMap.newKeySet();
    private long startupTime;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        this.startupTime = start;
        instance = this;
        startup();

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

    private void startup() {
        loadConfigs();

        ILifePlatform platform = new BungeePlatform(this);
        ServiceRegistry.register(ILifePlatform.class, platform);
        ServiceRegistry.register(IConfigurationService.class, new BungeeConfigurationService(config));
        ServiceRegistry.register(ILangService.class, new BungeeLangService(lang));

        setupRedis();

        this.databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();

        this.antiVPNService = new AntiVPNService(platform);
        ServiceRegistry.register(AntiVPNService.class, this.antiVPNService);
        getProxy().getPluginManager().registerListener(this, new BungeeAntiVPNListener(this, this.antiVPNService));

        this.antiAltManager = new BungeeAntiAltManager(this);
        this.reactionManager = new BungeeReactionManager(this);

        ServiceRegistry.register(ISanctionService.class, new SanctionService(databaseManager.getDatabaseProvider()));

        getProxy().getPluginManager().registerListener(this, new BungeeConnectionListener());
        getProxy().getPluginManager().registerListener(this, new BungeeChatListener());
        getProxy().getPluginManager().registerListener(this, new BungeeAntiAltListener(this));

        if (config.getBoolean("modules.staffchat.enabled", true)) {
            getProxy().getPluginManager().registerCommand(this, new BungeeStaffchatCommand(this));
        }

        getProxy().getPluginManager().registerCommand(this, new BungeeLifeModCommand(this));
    }

    private void setupRedis() {
        if (!config.getBoolean("redis.enabled", false)) return;

        String host = config.getString("redis.host", "localhost");
        int port = config.getInt("redis.port", 6379);
        String password = config.getString("redis.password", "");
        IMessagingService redis = new RedisMessagingService(host, port, password);
        ServiceRegistry.register(IMessagingService.class, redis);

        ILangService lang = ServiceRegistry.get(ILangService.class);
        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);

        redis.subscribe("lifemod:sanctions", message -> {
            try {
                int pipe = message.indexOf('|');
                String action = pipe > 0 ? message.substring(0, pipe) : "";
                String data = pipe > 0 ? message.substring(pipe + 1) : "";

                if ("ADD".equals(action)) {
                    String[] parts = data.split("\\|", -1);
                    if (parts.length < 9) return;
                    SanctionType type = SanctionType.valueOf(parts[0]);
                    UUID playerUuid = UUID.fromString(parts[1]);
                    String targetName = parts[2];
                    String issuerName = parts[3];
                    String reason = parts[4];
                    long duration = Long.parseLong(parts[5]);
                    boolean isSilent = Boolean.parseBoolean(parts[6]);
                    String origServer = parts[7];

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
                    if (parts.length < 6) return;
                    SanctionType type = SanctionType.valueOf(parts[0]);
                    UUID playerUuid = UUID.fromString(parts[1]);
                    String targetName = parts[2];
                    String removedByName = parts[3];
                    String reason = parts[4];
                    boolean silent = Boolean.parseBoolean(parts[5]);

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

        redis.subscribe("lifemod:staff", message -> {
            try {
                if (!config.getBoolean("modules.staffchat.enabled", true)) return;

                String[] parts = message.split("\\|");
                if (parts.length >= 5 && "CHAT".equals(parts[0])) {
                    String origServer = parts[4];
                    if ("BungeeCord".equals(origServer)) return;

                    String playerName = parts[2];
                    String chatMessage = parts[3];
                    String formatted = lang.getMessage("commands.staffchat.message",
                            "%player%", playerName,
                            "%message%", chatMessage,
                            "%server%", origServer);
                    TextComponent component = new TextComponent(lang.formatMessage(formatted));

                    ProxyServer.getInstance().getPlayers().stream()
                            .filter(p -> p.hasPermission("lifemod.staffchat"))
                            .filter(p -> !p.getServer().getInfo().getName().equals(origServer))
                            .forEach(p -> p.sendMessage(component));
                    ProxyServer.getInstance().getConsole().sendMessage(component);
                }
            } catch (Exception e) {
                getLogger().warning("Failed to process cross-server staffchat: " + message);
            }
        });
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

    public Set<UUID> getStaffChatToggled() {
        return staffChatToggled;
    }

    public BungeeReactionManager getReactionManager() {
        return reactionManager;
    }

    private void shutdown() {
        if (antiVPNService != null) antiVPNService.shutdown();
        IMessagingService msg = ServiceRegistry.get(IMessagingService.class);
        if (msg != null) msg.close();

        if (databaseManager != null) databaseManager.closeConnection();
    }

    public void fullReload() {
        getLogger().info("Performing full LifeMod Bungee reload...");

        shutdown();

        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getPluginManager().unregisterCommands(this);

        staffChatToggled.clear();

        startupTime = System.currentTimeMillis();
        instance = this;

        startup();

        getLogger().info("LifeMod Bungee fully reloaded successfully.");
    }

    @Override
    public void onDisable() {
        shutdown();
    }
}
