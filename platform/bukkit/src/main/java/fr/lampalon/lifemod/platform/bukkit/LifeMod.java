package fr.lampalon.lifemod.platform.bukkit;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.database.DatabaseManager;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.SanctionType;

import com.github.retrooper.packetevents.PacketEvents;
import com.zaxxer.hikari.HikariDataSource;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.messaging.RedisMessagingService;
import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.IPinService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.common.service.PinServiceImpl;
import fr.lampalon.lifemod.common.service.SanctionService;
import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.analytics.PostHogService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitItemsAdderService;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitLangService;
import fr.lampalon.lifemod.platform.bukkit.commands.engine.CommandRegistry;
import fr.lampalon.lifemod.platform.bukkit.adapter.IItemsAdderService;
import fr.lampalon.lifemod.platform.bukkit.listeners.*;
import fr.lampalon.lifemod.platform.bukkit.managers.*;
import fr.lampalon.lifemod.platform.bukkit.managers.gui.GuiManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.*;
import fr.lampalon.lifemod.platform.bukkit.nms.NMSLoader;
import fr.lampalon.lifemod.platform.bukkit.utils.ConfigUpdater;
import fr.lampalon.lifemod.platform.bukkit.webhook.BukkitWebhookService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.UpdateChecker;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.retrooper.packetevents.event.PacketListenerPriority;

public class LifeMod extends JavaPlugin {
    private static LifeMod instance;
    private SpectateManager spectateManager;
    private FreezeManager freezeManager;
    private DatabaseManager databaseManager;
    private UpdateChecker updateChecker;
    private DebugManager debugManager;
    private IPinService pinService;
    private ChatManager chatManager;
    private GuiManager guiManager;
    private NoteInputManager noteInputManager;
    private ModeratorSessionManager moderatorSessionManager;
    private ModeratorAuthService moderatorAuthService;
    private ReactionManager reactionManager;
    private StaffItemManager staffItemManager;
    private StaffModeManager staffModeManager;
    private InvseeManager invseeManager;
    private StaffActionManager staffActionManager;
    private fr.lampalon.lifemod.platform.bukkit.managers.ScanManager scanManager;
    private fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager noClipManager;
    private fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager replayPlayerManager;
    private fr.lampalon.lifemod.common.replay.ReplayManager replayManager;
    private IVanishService vanishService;
    private CommandRegistry commandRegistry;
    private fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager antiAltManager;
    private fr.lampalon.lifemod.common.antivpn.AntiVPNService antiVPNService;
    private fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayPacketListener replayPacketListener;
    private fr.lampalon.lifemod.common.antivpn.AntiVPNPacketListener antiVPNPacketListener;
    private fr.lampalon.lifemod.platform.bukkit.managers.staff.VanishPacketListener vanishPacketListener;
    private CPSListener cpsListener;
    private boolean chatEnabled = true;
    private FileConfiguration configConfig;
    private FileConfiguration langConfig;
    private Set<UUID> moderators = new HashSet<>();
    private final Map<UUID, Deque<Long>> cpsMap = new ConcurrentHashMap<>();
    private final Set<UUID> staffChatToggled = ConcurrentHashMap.newKeySet();
    private long startupTime;

    public static LifeMod getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        this.startupTime = start;
        instance = this;
        saveDefaultConfig();
        new ConfigUpdater(this).updateConfigs();
        loadConfigurations();

        BukkitPlatform bukkitPlatform = new BukkitPlatform(this);
        ServiceRegistry.register(ILifePlatform.class, bukkitPlatform);
        ServiceRegistry.register(IConfigurationService.class, new BukkitConfigurationService(configConfig));
        ServiceRegistry.register(ILangService.class, new BukkitLangService(langConfig));
        ServiceRegistry.register(IItemsAdderService.class, new BukkitItemsAdderService());

        setupRedis();

        PacketEvents.getAPI().init();
        bukkitPlatform.setNmsProvider(NMSLoader.load(getLogger()));

        String webhookUrl = configConfig.getString("modules.discord.webhook-url");
        boolean discordEnabled = configConfig.getBoolean("modules.discord.enabled", false);
        ServiceRegistry.register(IWebhookService.class,
                new BukkitWebhookService(webhookUrl, discordEnabled, getLogger()));
        this.spectateManager = new SpectateManager();
        this.debugManager = new DebugManager(this);
        initializeManagers();

        ServiceRegistry.register(IPinService.class, moderatorAuthService);

        PacketEvents.getAPI().getEventManager().registerListener(new fr.lampalon.lifemod.platform.bukkit.listeners.FreezePacketListener(this), PacketListenerPriority.NORMAL);

        this.commandRegistry = new CommandRegistry(this);
        registerEvents();
        registerCommands();
        setupMetrics();
        setupPostHog();

        // Start Replay Recorder
        new fr.lampalon.lifemod.platform.bukkit.replay.ReplayPositionRecorder(this).runTaskTimer(this, 2L, 2L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            databaseManager.getDatabaseProvider().cleanupExpiredSanctions();
        }, 20 * 60L, 20 * 60L);

        ServiceRegistry.register(ExecutorService.class, Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "LifeMod-Async");
            t.setDaemon(true);
            return t;
        }));

        long elapsed = System.currentTimeMillis() - start;
        printStartupMessage(elapsed, bukkitPlatform.getNmsProvider().getName());
    }

    private void setupRedis() {
        if (configConfig.getBoolean("redis.enabled", false)) {
            String host = configConfig.getString("redis.host");
            int port = configConfig.getInt("redis.port", 6379);
            String password = configConfig.getString("redis.password");
            IMessagingService redis = new RedisMessagingService(host, port, password);
            ServiceRegistry.register(IMessagingService.class, redis);

            String serverName = getServerName();
            ILangService lang = ServiceRegistry.get(ILangService.class);
            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);

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

                        if (origServer.equals(serverName)) return;

                        String targetName = Bukkit.getOfflinePlayer(playerUuid).getName();
                        if (targetName == null) targetName = playerUuid.toString().substring(0, 8);

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
                            Player online = Bukkit.getPlayer(playerUuid);
                            if (online != null && online.isOnline()) {
                                String kickReason = lang.getMessage("sanctions.ban.login",
                                        "%reason%", reason,
                                        "%issuer%", issuerName,
                                        "%expiration%", duration == 0 ? lang.getMessage("sanctions.permanent") : TimeUtil.formatTime(duration),
                                        "%id%", playerUuid.toString().substring(0, 8),
                                        "%server%", origServer);
                                String finalKickReason = kickReason;
                                Bukkit.getScheduler().runTask(this, () -> online.kickPlayer(finalKickReason));
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

                        String targetName = Bukkit.getOfflinePlayer(playerUuid).getName();
                        if (targetName == null) targetName = playerUuid.toString().substring(0, 8);

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
                    String[] parts = message.split("\\|");
                    if (parts.length >= 4 && "UPDATE".equals(parts[0])) {
                        UUID playerUuid = UUID.fromString(parts[1]);
                        boolean state = Boolean.parseBoolean(parts[2]);
                        String origServer = parts[3];

                        if (origServer.equals(serverName)) return;

                        if (state) {
                            moderators.add(playerUuid);
                        } else {
                            moderators.remove(playerUuid);
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to process cross-server staff update: " + message);
                }
            });
        }
    }

    private void loadConfigurations() {
        configConfig = loadConfig("config.yml");

        String langName = configConfig.getString("server.language", "en_US");
        langConfig = loadLanguageConfig(langName);
    }

    private FileConfiguration loadLanguageConfig(String langName) {
        File langFolder = new File(getDataFolder(), "languages");
        if (!langFolder.exists()) langFolder.mkdirs();

        File defaultLangFile = new File(langFolder, "en_US.yml");
        if (!defaultLangFile.exists()) {
            saveResource("languages/en_US.yml", false);
        }

        FileConfiguration baseConfig = new YamlConfiguration();
        try {
            baseConfig.load(defaultLangFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (langName.equals("en_US")) {
            getLogger().info("Using en_US directly (no merge needed)");
            return baseConfig;
        }

        File langFile = new File(langFolder, langName + ".yml");

        if (!langFile.exists()) {
            String resourcePath = "languages/" + langName + ".yml";
            if (getResource(resourcePath) != null) {
                saveResource(resourcePath, false);
            } else {
                getLogger().warning("Language '" + langName + "' not found. Using en_US.");
                return baseConfig;
            }
        }

        FileConfiguration specificConfig = new YamlConfiguration();
        try {
            specificConfig.load(langFile);
        } catch (Exception e) {
            getLogger().severe("Failed to load " + langName + ".yml");
            e.printStackTrace();
            return baseConfig;
        }

        int specificKeys = 0;

        FileConfiguration mergedConfig = new YamlConfiguration();

        for (String key : baseConfig.getKeys(true)) {
            mergedConfig.set(key, baseConfig.get(key));
        }

        for (String key : specificConfig.getKeys(true)) {
            if (!specificConfig.isConfigurationSection(key)) {
                Object val = specificConfig.get(key);

                if (val != null) {
                    String stringValue;
                    try {
                        stringValue = val.toString();
                    } catch (Exception e) {
                        getLogger().warning("Error of conversion of key " + key);
                        continue;
                    }

                    if (stringValue != null && !stringValue.trim().isEmpty()) {
                        mergedConfig.set(key, stringValue);
                    }
                }
            }
        }
        return mergedConfig;
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            getDataFolder().mkdirs();
            saveResource(fileName, false);
        }
        FileConfiguration config = new YamlConfiguration();
        try { config.load(file); } catch (Exception e) { e.printStackTrace(); }
        return config;
    }

    private void initializeManagers() {
        freezeManager = new FreezeManager();
        chatManager = new ChatManager(this);
        databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();
        DatabaseProvider dbProvider = databaseManager.getDatabaseProvider();
        ServiceRegistry.register(DatabaseProvider.class, dbProvider);
        ServiceRegistry.register(ISanctionService.class, new SanctionService(dbProvider));
        
        guiManager = new GuiManager(this);
        noteInputManager = new NoteInputManager(this);
        moderatorAuthService = new ModeratorAuthService(this);
        moderatorSessionManager = new ModeratorSessionManager(configConfig.getInt("modules.moderator-auth.max-attempts", 3));
        reactionManager = new ReactionManager(this);

        staffItemManager = new StaffItemManager(this);
        staffModeManager = new StaffModeManager(this, staffItemManager);
        invseeManager = new InvseeManager();
        staffActionManager = new StaffActionManager();
        scanManager = new fr.lampalon.lifemod.platform.bukkit.managers.ScanManager(this);
        noClipManager = new NoClipManager(this);
        getServer().getPluginManager().registerEvents(
                new NoClipBukkitListener(noClipManager), this
        );
        antiAltManager = new fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager(this);

        this.replayPlayerManager = new fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager(this);
        this.replayManager = new fr.lampalon.lifemod.common.replay.ReplayManager();
        this.replayPacketListener = new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayPacketListener(this.replayManager);
        PacketEvents.getAPI().getEventManager().registerListener(
                replayPacketListener,
                PacketListenerPriority.MONITOR
        );

        // AntiVPN System
        this.antiVPNService = new fr.lampalon.lifemod.common.antivpn.AntiVPNService(ServiceRegistry.get(ILifePlatform.class));
        this.antiVPNPacketListener = new fr.lampalon.lifemod.common.antivpn.AntiVPNPacketListener(this.antiVPNService, ServiceRegistry.get(ILifePlatform.class));
        PacketEvents.getAPI().getEventManager().registerListener(
                antiVPNPacketListener,
                PacketListenerPriority.LOW
        );
        ServiceRegistry.register(fr.lampalon.lifemod.common.antivpn.AntiVPNService.class, this.antiVPNService);

        vanishService = new VanishService(this);
        this.vanishPacketListener = new fr.lampalon.lifemod.platform.bukkit.managers.staff.VanishPacketListener(vanishService);
        PacketEvents.getAPI().getEventManager().registerListener(
                vanishPacketListener,
                PacketListenerPriority.HIGH
        );
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 19817);
        metrics.addCustomChart(new SingleLineChart("players", () -> Bukkit.getOnlinePlayers().size()));
    }

    private void setupPostHog() {
        if (!configConfig.getBoolean("modules.posthog.enabled", true)) return;

        String host = "https://eu.posthog.com";

        getLogger().info("PostHog: resolving API key...");
        String apiKey = PostHogService.resolveApiKey();
        String serverVersion = Bukkit.getBukkitVersion();
        String javaVersion = System.getProperty("java.version");
        String dbType = configConfig.getString("database.type", "sqlite");
        boolean redis = configConfig.getBoolean("redis.enabled", false);
        int maxPlayers = Bukkit.getMaxPlayers();

        getLogger().info("PostHog: creating service (host=" + host + ")...");
        try {
            PostHogService service = new PostHogService(apiKey, getServerName(), getDescription().getVersion(), "bukkit", host, serverVersion, javaVersion, dbType, redis, maxPlayers, getDataFolder().getPath());
            ServiceRegistry.register(IPostHogService.class, service);
            getLogger().info("PostHog: registered in ServiceRegistry");

            sendEnvironmentEvent(service);
            sendConfigSnapshot(service);

            getLogger().info("PostHog analytics enabled");
        } catch (Exception e) {
            getLogger().severe("PostHog FAILED to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendEnvironmentEvent(PostHogService service) {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        service.sendEnvironmentEvent(
                os.getName(), os.getArch(), os.getVersion(),
                runtime.availableProcessors(),
                runtime.maxMemory() / 1048576,
                runtime.totalMemory() / 1048576
        );
    }

    private void sendConfigSnapshot(PostHogService service) {
        Map<String, Boolean> modules = new HashMap<>();
        modules.put("auto_punish", configConfig.getBoolean("modules.auto-punish.enabled", true));
        modules.put("chat_manager", configConfig.getBoolean("modules.chat-manager.enabled", true));
        modules.put("moderator_auth", configConfig.getBoolean("modules.moderator-auth.enabled", false));
        modules.put("antialt", configConfig.getBoolean("modules.antialt.enabled", true));
        modules.put("antivpn", configConfig.getBoolean("modules.antivpn.enabled", false));
        modules.put("discord", configConfig.getBoolean("modules.discord.enabled", false));
        modules.put("replay", true);

        Map<String, Object> extra = new HashMap<>();
        extra.put("database_type", configConfig.getString("database.type", "sqlite"));
        extra.put("redis_enabled", configConfig.getBoolean("redis.enabled", false));
        extra.put("commands_enabled_count", countEnabledCommands());
        extra.put("antivpn_geo_mode", configConfig.getString("modules.antivpn.geo-blocking.mode", "NONE"));
        extra.put("auto_punish_mode", configConfig.getString("modules.auto-punish.mode", "GLOBAL"));

        service.sendConfigSnapshot(modules, extra);
    }

    private int countEnabledCommands() {
        int count = 0;
        for (String key : configConfig.getConfigurationSection("commands.enabled").getKeys(false)) {
            if (configConfig.getBoolean("commands.enabled." + key, false)) count++;
        }
        return count;
    }

    private void registerEvents() {
        PluginManager pm = Bukkit.getPluginManager();
        updateChecker = new UpdateChecker(this, 112381);
        pm.registerEvents(new ModCancels(), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffListener(staffModeManager, staffItemManager, staffActionManager), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffPhysicalListener(staffModeManager), this);
        pm.registerEvents(new StaffChatEvent(this), this);
        pm.registerEvents(new PluginDisable(), this);
        pm.registerEvents(new PlayerQuit(this), this);
        pm.registerEvents(new PlayerTeleportEvent(), this);

        cpsListener = new CPSListener(cpsMap);
        pm.registerEvents(cpsListener, this);
        PacketEvents.getAPI().getEventManager().registerListener(cpsListener, PacketListenerPriority.NORMAL);

        pm.registerEvents(new GuiDetailListener(this), this);
        pm.registerEvents(new ChatAsyncListener(this), this);
        //pm.registerEvents(new TicketJoinListener(this, updateChecker), this);
        pm.registerEvents(new ChatListener(), this);
        pm.registerEvents(new ModeratorAuthListener(), this);
        pm.registerEvents(new SanctionListener(), this);
        pm.registerEvents(new ConnectionListener(), this);
        pm.registerEvents(new InvseeListener(this), this);
        pm.registerEvents(new AntiAltListener(this), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayInteractionListener(this), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayAutoStartListener(this), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayBlockListener(replayManager), this);
        pm.registerEvents(new PlayerJoin(this, updateChecker), this);
    }

    private void registerCommands() {
        commandRegistry.scanAndRegisterCommands("fr.lampalon.lifemod.platform.bukkit.commands.impl");
    }

    private void printStartupMessage(long elapsed, String nmsVersion) {
        getLogger().info("&8&m----------------------------------------");
        getLogger().info("&6&lLifeMod &7- &aSuccessfully Enabled");
        getLogger().info(" ");
        getLogger().info("&e• &fVersion: &b" + getDescription().getVersion());
        getLogger().info("&e• &fPlatform: &b" + nmsVersion);
        getLogger().info("&e• &fDatabase: &a" + configConfig.getString("database.type").toUpperCase());
        getLogger().info("&e• &fCommands: &aAuto-Registered (" + commandRegistry.getCommands().size() + ")");
        getLogger().info("&e• &fStartup Time: &e" + elapsed + "ms");
        getLogger().info("&8&m----------------------------------------");
    }

    @Override
    public void onDisable() {
        noClipManager.shutdown();
        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            java.util.Map<String, Object> props = new java.util.HashMap<>();
            props.put("plugin_version", getDescription().getVersion());
            props.put("platform", "bukkit");
            props.put("uptime_seconds", (System.currentTimeMillis() - startupTime) / 1000);
            ph.capture("lifemod_shutdown", props);
            ph.shutdown();
        }
        PacketEvents.getAPI().terminate();
        IMessagingService msg = ServiceRegistry.get(IMessagingService.class);
        if (msg != null) msg.close();
        if (databaseManager != null) databaseManager.closeConnection();
    }

    public FileConfiguration getLangConfig() { return langConfig; }
    public FileConfiguration getConfigConfig() { return configConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ChatManager getChatManager() { return chatManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public NoteInputManager getNoteInputManager() { return noteInputManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public IVanishService getVanishService() { return vanishService; }
    public DebugManager getDebugManager() { return debugManager; }
    public SpectateManager getSpectateManager() { return spectateManager; }
    public ModeratorSessionManager getModeratorSessionManager() { return moderatorSessionManager; }
    public ModeratorAuthService getModeratorAuthService() { return moderatorAuthService; }
    public ReactionManager getReactionManager() { return reactionManager; }
    public Map<UUID, Deque<Long>> getCpsMap() {
        return cpsMap;
    }

    public Set<UUID> getStaffChatToggled() {
        return staffChatToggled;
    }
    public StaffModeManager getStaffModeManager() { return staffModeManager; }
    public InvseeManager getInvseeManager() { return invseeManager; }
    public fr.lampalon.lifemod.platform.bukkit.managers.ScanManager getScanManager() { return scanManager; }
    public fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager getNoClipManager() { return noClipManager; }
    public fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager getAntiAltManager() { return antiAltManager; }
    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
    public Set<UUID> getModerators() { return moderators; }
    public boolean isFreeze(Player p) { return freezeManager.isPlayerFrozen(p.getUniqueId()); }
    public Map<UUID, Location> getFrozenPlayers() { return freezeManager.getFrozenPlayers(); }
    public void reloadPluginConfig() {
        configConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        debugManager.reloadCache();
        if (staffModeManager != null) staffModeManager.reloadEffectsConfig();
    }

    public fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager getReplayPlayerManager() {
        return replayPlayerManager;
    }

    public ReplayManager getReplayManager() {
        return replayManager;
    }

    public String getServerName() {
        return configConfig.getString("server.name");
    }
}
