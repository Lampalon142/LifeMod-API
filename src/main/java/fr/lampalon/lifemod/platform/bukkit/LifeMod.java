package fr.lampalon.lifemod.platform.bukkit;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.database.DatabaseManager;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.SanctionType;

import com.github.retrooper.packetevents.PacketEvents;
import com.zaxxer.hikari.HikariDataSource;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.messaging.RedisMessagingService;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.SanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.integration.nms.PacketController;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitLangService;
import fr.lampalon.lifemod.platform.bukkit.commands.*;
import fr.lampalon.lifemod.platform.bukkit.commands.adapter.BukkitCommandAdapter;
import fr.lampalon.lifemod.platform.bukkit.listeners.*;
import fr.lampalon.lifemod.platform.bukkit.managers.*;
import fr.lampalon.lifemod.platform.bukkit.managers.gui.GuiManager;
import fr.lampalon.lifemod.platform.bukkit.utils.ConfigUpdater;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.UpdateChecker;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;

import com.github.retrooper.packetevents.event.PacketListenerPriority;

public class LifeMod extends JavaPlugin {
    private static LifeMod instance;
    private CommandMap commandMap;
    private SpectateManager spectateManager;
    private FreezeManager freezeManager;
    private DatabaseManager databaseManager;
    private UpdateChecker updateChecker;
    private DebugManager debugManager;
    private ChatManager chatManager;
    private VanishedManager playerManager;
    private GuiManager guiManager;
    private NoteInputManager noteInputManager;
    private ModeratorSessionManager moderatorSessionManager;
    private ModeratorAuthService moderatorAuthService;
    private PacketController packetController;
    private boolean chatEnabled = true;
    private FileConfiguration configConfig;
    private FileConfiguration langConfig;
    private Set<UUID> moderators = new HashSet<>();
    private Map<UUID, PlayerManager> players = new HashMap<>();
    private final Map<UUID, Deque<Long>> cpsMap = new HashMap<>();
    public String webHookUrl;

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
        instance = this;
        saveDefaultConfig();
        new ConfigUpdater(this).updateConfigs();
        loadConfigurations();
        
        ILifePlatform platform = new BukkitPlatform(this);
        ServiceRegistry.register(ILifePlatform.class, platform);
        ServiceRegistry.register(IConfigurationService.class, new BukkitConfigurationService(configConfig));
        ServiceRegistry.register(ILangService.class, new BukkitLangService(langConfig));
        
        if (configConfig.getBoolean("redis.enabled", false)) {
            String host = configConfig.getString("redis.host", "localhost");
            int port = configConfig.getInt("redis.port", 6379);
            String password = configConfig.getString("redis.password", "");
            IMessagingService redis = new RedisMessagingService(host, port, password);
            ServiceRegistry.register(IMessagingService.class, redis);
            
            redis.subscribe("lifemod:sanctions", message -> {
                String[] parts = message.split("\\|");
                if (parts.length < 3) return;
                
                String action = parts[0];
                String typeStr = parts[1];
                UUID playerUuid = UUID.fromString(parts[2]);
                
                if (action.equals("ADD")) {
                    // ADD|TYPE|PLAYER_UUID|ISSUER_NAME|REASON|DURATION|SILENT|SERVER|CATEGORY
                    if (parts.length < 9) return;
                    String issuerName = parts[3];
                    String reason = parts[4];
                    long duration = Long.parseLong(parts[5]);
                    boolean silent = Boolean.parseBoolean(parts[6]);
                    String server = parts[7];
                    String category = parts[8];

                    Bukkit.getScheduler().runTask(this, () -> {
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null) {
                            if (typeStr.equals("BAN")) {
                                String kickMsg = langConfig.getString("sanctions.ban.login", "&cYou have been banned!\n\nReason: &f%reason%")
                                        .replace("%reason%", reason);
                                player.kickPlayer(MessageUtil.formatMessage(kickMsg));
                            } else if (typeStr.equals("KICK")) {
                                String kickMsg = langConfig.getString("sanctions.kick.message", "&cYou have been kicked!\n\nReason: &f%reason%")
                                        .replace("%reason%", reason);
                                player.kickPlayer(MessageUtil.formatMessage(kickMsg));
                            }
                        }

                        // Broadcast sur les autres serveurs
                        String path = "sanctions.broadcast." + typeStr.toLowerCase() + (silent ? ".silent" : ".public");
                        String template = langConfig.getString(path);
                        if (template != null) {
                            String targetName = Bukkit.getOfflinePlayer(playerUuid).getName();
                            if (targetName == null) targetName = playerUuid.toString();

                            String broadcastMsg = template
                                    .replace("%target%", targetName)
                                    .replace("%issuer%", issuerName)
                                    .replace("%reason%", reason)
                                    .replace("%time%", TimeUtil.formatTime(duration))
                                    .replace("%server%", server);

                            String formatted = MessageUtil.formatMessage(broadcastMsg);
                            if (silent) {
                                Bukkit.getOnlinePlayers().stream()
                                        .filter(p -> p.hasPermission("lifemod.sanctions.see-silent"))
                                        .forEach(p -> p.sendMessage(formatted));
                                Bukkit.getConsoleSender().sendMessage(formatted);
                            } else {
                                Bukkit.broadcastMessage(formatted);
                            }
                        }
                    });
                } else if (action.equals("REMOVE")) {
                    // REMOVE|TYPE|PLAYER_UUID|REMOVED_BY_NAME|REASON|SILENT
                    if (parts.length < 5) return;
                    String removedByName = parts[3];
                    String reason = parts[4];
                    boolean silent = parts.length > 5 && Boolean.parseBoolean(parts[5]);

                    Bukkit.getScheduler().runTask(this, () -> {
                        String path = "sanctions.broadcast.un" + typeStr.toLowerCase() + (silent ? ".silent" : ".public");
                        String template = langConfig.getString(path);
                        if (template == null) {
                            template = langConfig.getString("sanctions.broadcast.un" + typeStr.toLowerCase());
                        }
                        
                        if (template != null) {
                            String targetName = Bukkit.getOfflinePlayer(playerUuid).getName();
                            if (targetName == null) targetName = playerUuid.toString();

                            String broadcastMsg = template
                                    .replace("%target%", targetName)
                                    .replace("%issuer%", removedByName)
                                    .replace("%reason%", reason);

                            String formatted = MessageUtil.formatMessage(broadcastMsg);
                            if (silent) {
                                Bukkit.getOnlinePlayers().stream()
                                        .filter(p -> p.hasPermission("lifemod.sanctions.see-silent"))
                                        .forEach(p -> p.sendMessage(formatted));
                                Bukkit.getConsoleSender().sendMessage(formatted);
                            } else {
                                Bukkit.broadcastMessage(formatted);
                            }
                        }
                    });
                }
            });
        }

        PacketEvents.getAPI().init();
        
        this.webHookUrl = configConfig.getString("modules.discord.webhook-url");
        this.spectateManager = new SpectateManager();
        this.debugManager = new DebugManager(this);
        initializeManagers();
        
        ServiceRegistry.register(ISanctionService.class, new SanctionService(databaseManager.getDatabaseProvider()));

        this.packetController = new PacketController(this);
        PacketEvents.getAPI().getEventManager().registerListener(this.packetController, PacketListenerPriority.NORMAL);
        
        registerEvents();
        registerCommands();
        setupMetrics();

        // Cleanup task for expired sanctions
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            databaseManager.getDatabaseProvider().cleanupExpiredSanctions();
        }, 20 * 60L, 20 * 60L);
        
        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("Started in " + elapsed + "ms");
    }

    private void loadConfigurations() {
        configConfig = loadConfig("config.yml");
        langConfig = loadConfig("lang.yml");
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
        playerManager = new VanishedManager();
        chatManager = new ChatManager(this);
        databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();
        guiManager = new GuiManager(this);
        noteInputManager = new NoteInputManager(this);
        moderatorAuthService = new ModeratorAuthService(this);
        moderatorSessionManager = new ModeratorSessionManager(configConfig.getInt("modules.moderator-auth.max-attempts", 3));
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 19817);
        metrics.addCustomChart(new SingleLineChart("players", () -> Bukkit.getOnlinePlayers().size()));
    }

    private void registerEvents() {
        PluginManager pm = Bukkit.getPluginManager();
        updateChecker = new UpdateChecker(this, 112381);
        pm.registerEvents(new ModCancels(), this);
        pm.registerEvents(new ModItemsInteract(), this);
        pm.registerEvents(new Staffchatevent(this), this);
        pm.registerEvents(new PluginDisable(), this);
        pm.registerEvents(new PlayerQuit(), this);
        pm.registerEvents(new PlayerTeleportEvent(), this);
        pm.registerEvents(new CPSListener(cpsMap), this);
        pm.registerEvents(new GuiDetailListener(this), this);
        pm.registerEvents(new ChatAsyncListener(this), this);
        pm.registerEvents(new TicketJoinListener(this, updateChecker), this);
        pm.registerEvents(new ChatListener(), this);
        pm.registerEvents(new ModeratorAuthListener(), this);
        pm.registerEvents(new SanctionListener(), this);
        pm.registerEvents(new ConnectionListener(), this);
        if (langConfig.getBoolean("system.update.enabled")) {
            pm.registerEvents(new PlayerJoin(this, updateChecker), this);
        }
    }

    private void registerCommands() {
        registerCommand(new FlyCmd(this));
        registerCommand(new BanCmd());
        registerCommand(new MuteCmd());
        registerCommand(new KickCmd());
        registerCommand(new WarnCmd());
        registerCommand(new NoteCmd());
        registerCommand(new UnbanCmd());
        registerCommand(new UnmuteCmd());
        registerCommand(new HistoryCmd());
        registerCommand(new CaseCmd());
        registerCommand(new AltsCmd());
        registerCommand(new StaffHistoryCmd());
        
        registerCommand("freeze", new FreezeCmd(this));
        registerCommand("mod", new ModCmd(this));
        registerCommand("staff", new ModCmd(this));
        registerCommand("broadcast", new BroadcastCmd(this));
        registerCommand("bc", new BroadcastCmd(this));
        registerCommand("gamemode", new GmCmd(this));
        registerCommand("gm", new GmCmd(this));
        registerCommand("ecopen", new EcopenCmd(this));
        registerCommand("vanish", new VanishCmd(playerManager));
        registerCommand("clearinv", new ClearinvCmd(this));
        registerCommand("stafflist", new StafflistCmd());
        registerCommand("staffchat", new StaffchatCmd());
        registerCommand("chatclear", new ChatclearCmd(this));
        registerCommand("heal", new HealCmd(this));
        registerCommand("tp", new TeleportCmd());
        registerCommand("tphere", new TeleportCmd());
        registerCommand("god", new GodModCmd(this));
        registerCommand("invsee", new InvseeCmd(this));
        registerCommand("feed", new FeedCmd(this));
        registerCommand("weather", new WeatherCmd(this));
        registerCommand("lifemod", new LifemodCmd(this));
        registerCommand("speed", new SpeedCmd());
        registerCommand("spectate", new SpectateCmd(this));
        registerCommand("otp", new OtpCmd(databaseManager));
        registerCommand("oinvsee", new OInvseeCmd(databaseManager));
        registerCommand("settime", new TimeCmd(this));
        registerCommand("difficulty", new DifficultyCmd(this));
        registerCommand("hearts", new HeartsCmd(this));
        registerCommand("modregister", new ModRegisterCmd());
        registerCommand("modlogin", new ModLoginCmd());
        registerCommand("modreset", new ModResetCmd());
        registerCommand("modchangepass", new ModChangePassCmd());
        registerCommand("follow", new FollowCmd(this));
        registerCommand("report", new ReportCmd(this));
        registerCommand("reports", new ReportsCmd(this));
        registerCommand("togglechat", new ToggleChatCmd());
    }

    private void registerCommand(LifeCommand lifeCommand) {
        registerCommand(lifeCommand.getName(), new BukkitCommandAdapter(lifeCommand));
    }

    private void registerCommand(String commandName, CommandExecutor executor) {
        if (configConfig.getBoolean("commands.enabled." + commandName, true)) {
            if (getCommand(commandName) != null) {
                getCommand(commandName).setExecutor(executor);
                if (executor instanceof TabCompleter) getCommand(commandName).setTabCompleter((TabCompleter) executor);
            }
        }
    }

    @Override
    public void onDisable() {
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
    public PacketController getPacketController() { return packetController; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public VanishedManager getPlayerManager() { return playerManager; }
    public DebugManager getDebugManager() { return debugManager; }
    public SpectateManager getSpectateManager() { return spectateManager; }
    public ModeratorSessionManager getModeratorSessionManager() { return moderatorSessionManager; }
    public ModeratorAuthService getModeratorAuthService() { return moderatorAuthService; }
    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
    public Map<UUID, PlayerManager> getPlayers() { return players; }
    public Set<UUID> getModerators() { return moderators; }
    public boolean isFreeze(Player p) { return freezeManager.isPlayerFrozen(p.getUniqueId()); }
    public Map<UUID, Location> getFrozenPlayers() { return freezeManager.getFrozenPlayers(); }
    public void reloadPluginConfig() { configConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml")); }
    public void reloadLangConfig() { langConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml")); }
    
    @Override
    public String getServerName() {
        return configConfig.getString("server.name", "Survival");
    }
}
}

