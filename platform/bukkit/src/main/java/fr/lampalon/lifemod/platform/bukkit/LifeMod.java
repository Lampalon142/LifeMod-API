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
import fr.lampalon.lifemod.common.service.ILogService;
import fr.lampalon.lifemod.common.service.IPinService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.common.service.LogService;
import fr.lampalon.lifemod.common.service.PinServiceImpl;
import fr.lampalon.lifemod.common.service.SanctionService;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.PacketEvents;
import com.zaxxer.hikari.HikariDataSource;
import fr.lampalon.lifemod.common.antivpn.AntiVPNPacketListener;
import fr.lampalon.lifemod.common.antivpn.AntiVPNService;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseManager;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.messaging.RedisMessagingService;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.replay.ReplayManager;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ILogService;
import fr.lampalon.lifemod.common.service.IPinService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.common.service.LogService;
import fr.lampalon.lifemod.common.service.PinServiceImpl;
import fr.lampalon.lifemod.common.service.SanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitItemsAdderService;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitLangService;
import fr.lampalon.lifemod.platform.bukkit.adapter.IItemsAdderService;
import fr.lampalon.lifemod.platform.bukkit.commands.engine.BukkitCommandWrapper;
import fr.lampalon.lifemod.platform.bukkit.commands.engine.CommandRegistry;
import fr.lampalon.lifemod.platform.bukkit.listeners.*;
import fr.lampalon.lifemod.platform.bukkit.listeners.hooks.*;
import fr.lampalon.lifemod.platform.bukkit.managers.*;
import fr.lampalon.lifemod.platform.bukkit.managers.LogCommandInterceptor;
import fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.*;
import fr.lampalon.lifemod.api.LifeModAPI;
import fr.lampalon.lifemod.nms.NmsFactory;
import fr.lampalon.lifemod.platform.bukkit.api.LifeModAPIImpl;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayPositionRecorder;
import fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayAutoStartListener;
import fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayBlockListener;
import fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayInteractionListener;
import fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayPacketListener;
import fr.lampalon.lifemod.platform.bukkit.utils.ConfigUpdater;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.UpdateChecker;
import fr.lampalon.lifemod.platform.bukkit.webhook.BukkitWebhookService;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LifeMod extends JavaPlugin {
    private static LifeMod instance;
    private static LifeModAPI lifeModAPI;
    private FreezeManager freezeManager;
    private DatabaseManager databaseManager;
    private UpdateChecker updateChecker;
    private DebugManager debugManager;
    private IPinService pinService;
    private ChatManager chatManager;
    private ModeratorSessionManager moderatorSessionManager;
    private ModeratorAuthService moderatorAuthService;
    private ReactionManager reactionManager;
    private StaffItemManager staffItemManager;
    private StaffModeManager staffModeManager;
    private InvseeManager invseeManager;
    private StaffActionManager staffActionManager;
    private fr.lampalon.lifemod.platform.bukkit.managers.ScanManager scanManager;
    private fr.lampalon.lifemod.platform.bukkit.managers.NoClipManager noClipManager;
    private fr.lampalon.lifemod.platform.bukkit.listeners.TraceItemListener traceItemListener;
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

    public static LifeModAPI getAPI() {
        return lifeModAPI;
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
        startup();
        long elapsed = System.currentTimeMillis() - start;
        printStartupMessage(elapsed, ServiceRegistry.get(ILifePlatform.class).getNmsProvider().getName());
    }

    private void shutdown() {
        if (noClipManager != null) noClipManager.shutdown();
        ILogService logSvc = ServiceRegistry.get(ILogService.class);
        if (logSvc != null) logSvc.shutdown();
        PacketEvents.getAPI().terminate();
        IMessagingService msg = ServiceRegistry.get(IMessagingService.class);
        if (msg != null) msg.close();
        if (databaseManager != null) databaseManager.closeConnection();
    }

    private void startup() {
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
        bukkitPlatform.setNmsProvider(NmsFactory.load(getLogger(), this));
        lifeModAPI = new LifeModAPIImpl(this);

        String webhookUrl = configConfig.getString("modules.discord.webhook-url");
        boolean discordEnabled = configConfig.getBoolean("modules.discord.enabled", false);
        ServiceRegistry.register(IWebhookService.class,
                new BukkitWebhookService(webhookUrl, discordEnabled, getLogger()));

        this.debugManager = new DebugManager(this);
        ServiceRegistry.register(ExecutorService.class, Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "LifeMod-Async");
            t.setDaemon(true);
            return t;
        }));
        initializeManagers();

        ServiceRegistry.register(IPinService.class, moderatorAuthService);

        PacketEvents.getAPI().getEventManager().registerListener(new FreezePacketListener(this), PacketListenerPriority.NORMAL);

        if (ServiceRegistry.get(ILogService.class) != null) {
            try {
                PacketEvents.getAPI().getEventManager().registerListener(
                    new LogCommandInterceptor(), PacketListenerPriority.LOW);
            } catch (Exception e) {
                getLogger().warning("Failed to register LogCommandInterceptor: " + e.getMessage());
            }
        }

        this.commandRegistry = new CommandRegistry(this);
        registerEvents();
        registerCommands();
        setupMetrics();

        new ReplayPositionRecorder(this).runTaskTimer(this, 2L, 2L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            databaseManager.getDatabaseProvider().cleanupExpiredSanctions();
        }, 20 * 60L, 20 * 60L);
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
                        if (parts.length < 9) return;
                        SanctionType type = SanctionType.valueOf(parts[0]);
                        UUID playerUuid = UUID.fromString(parts[1]);
                        String targetName = parts[2];
                        String issuerName = parts[3];
                        String reason = parts[4];
                        long duration = Long.parseLong(parts[5]);
                        boolean isSilent = Boolean.parseBoolean(parts[6]);
                        String origServer = parts[7];

                        if (origServer.equals(serverName)) return;

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

            redis.subscribe("lifemod:reload", message -> {
                try {
                    getLogger().info("Received cross-server reload request, reloading...");
                    fullReload();
                } catch (Exception e) {
                    getLogger().warning("Failed to process cross-server reload: " + message);
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

        if (configConfig.getBoolean("logs.enabled", true)) {
            try {
                ServiceRegistry.register(ILogService.class,
                    new LogService(dbProvider, ServiceRegistry.get(IConfigurationService.class), ServiceRegistry.get(ExecutorService.class)));
            } catch (Exception e) {
                getLogger().severe("Failed to initialize LogService: " + e.getMessage());
            }
        }

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
        this.replayManager.cleanupExpiredReplays();
        new BukkitRunnable() {
            @Override
            public void run() {
                replayManager.cleanupExpiredReplays();
            }
        }.runTaskTimer(this, 36000L, 36000L); // every 30 minutes (on the main thread, safe for SQLite/MySQL)
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

        pm.registerEvents(new ChatListener(), this);
        pm.registerEvents(new ModeratorAuthListener(), this);
        pm.registerEvents(new SanctionListener(), this);
        pm.registerEvents(new ConnectionListener(), this);
        pm.registerEvents(new InvseeListener(this), this);
        pm.registerEvents(new AntiAltListener(this), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayInteractionListener(this), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayAutoStartListener(this), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayJoinListener(replayManager.getSkinManager()), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayBlockListener(replayManager), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.replay.listeners.ReplayEventRecorder(replayManager), this);
        pm.registerEvents(new PlayerJoin(this, updateChecker), this);
        pm.registerEvents(new ServerListPingListener(vanishService), this);
        pm.registerEvents(new ReportListener(this), this);

        if (ServiceRegistry.get(ILogService.class) != null) {
            pm.registerEvents(new LogConnectionListener(), this);
            pm.registerEvents(new LogChatListener(), this);
            pm.registerEvents(new LogDeathListener(), this);
            pm.registerEvents(new LogBlockListener(), this);
            pm.registerEvents(new LogContainerListener(), this);
            pm.registerEvents(new LogItemListener(), this);
            pm.registerEvents(new LogEntityListener(), this);
            pm.registerEvents(new LogMovementListener(), this);
            traceItemListener = new TraceItemListener();
            pm.registerEvents(traceItemListener, this);
            pm.registerEvents(new VaultHook(), this);
            pm.registerEvents(new AxTradesHook(), this);
            pm.registerEvents(new QuickShopHook(), this);
            pm.registerEvents(new ChestShopHook(), this);
            pm.registerEvents(new ShopKeepersHook(), this);
        }
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
        shutdown();
    }

    public FileConfiguration getLangConfig() { return langConfig; }
    public FileConfiguration getConfigConfig() { return configConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ChatManager getChatManager() { return chatManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public IVanishService getVanishService() { return vanishService; }
    public DebugManager getDebugManager() { return debugManager; }
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

    public fr.lampalon.lifemod.platform.bukkit.listeners.TraceItemListener getTraceItemListener() { return traceItemListener; }
    public fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager getAntiAltManager() { return antiAltManager; }
    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
    public Set<UUID> getModerators() { return moderators; }
    public boolean isFreeze(Player p) { return freezeManager.isPlayerFrozen(p.getUniqueId()); }
    public Map<UUID, Location> getFrozenPlayers() { return freezeManager.getFrozenPlayers(); }
    public void fullReload() {
        getLogger().info("Performing full LifeMod reload...");

        shutdown();

        HandlerList.unregisterAll(this);

        try {
            Field cmdField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdField.setAccessible(true);
            CommandMap cmdMap = (CommandMap) cmdField.get(Bukkit.getServer());
            Field knownField = cmdMap.getClass().getDeclaredField("knownCommands");
            knownField.setAccessible(true);
            Map<String, org.bukkit.command.Command> known = (Map<String, org.bukkit.command.Command>) knownField.get(cmdMap);
            known.values().removeIf(cmd -> cmd instanceof BukkitCommandWrapper);
            known.values().removeIf(cmd ->
                cmd instanceof org.bukkit.command.PluginCommand &&
                ((org.bukkit.command.PluginCommand) cmd).getPlugin() == this
            );
        } catch (Exception e) {
            getLogger().warning("Failed to unregister commands: " + e.getMessage());
        }

        staffChatToggled.clear();
        moderators.clear();
        cpsMap.clear();

        startupTime = System.currentTimeMillis();
        instance = this;

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        startup();

        getLogger().info("LifeMod fully reloaded successfully.");
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
