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
import fr.lampalon.lifemod.platform.bukkit.managers.staff.*;
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
    private GuiManager guiManager;
    private NoteInputManager noteInputManager;
    private ModeratorSessionManager moderatorSessionManager;
    private ModeratorAuthService moderatorAuthService;
    private PacketController packetController;
    private ReactionManager reactionManager;
    private StaffItemManager staffItemManager;
    private StaffModeManager staffModeManager;
    private InvseeManager invseeManager;
    private StaffActionManager staffActionManager;
    private IVanishService vanishService;
    private fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager antiAltManager;
    private boolean chatEnabled = true;
    private FileConfiguration configConfig;
    private FileConfiguration langConfig;
    private Set<UUID> moderators = new HashSet<>();
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
            String host = configConfig.getString("redis.host");
            int port = configConfig.getInt("redis.port", 6379);
            String password = configConfig.getString("redis.password");
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
                                String kickMsg = langConfig.getString("sanctions.ban.login")
                                        .replace("%reason%", reason);
                                player.kickPlayer(MessageUtil.formatMessage(kickMsg));
                            } else if (typeStr.equals("KICK")) {
                                String kickMsg = langConfig.getString("sanctions.kick.message")
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

            redis.subscribe("lifemod:staff", message -> {
                String[] parts = message.split("\\|");
                if (parts.length < 4) return;

                String action = parts[0];
                if (!action.equals("UPDATE")) return;

                UUID uuid = UUID.fromString(parts[1]);
                boolean state = Boolean.parseBoolean(parts[2]);
                String originServer = parts[3];

                // If the message comes from THIS server, ignore it
                if (originServer.equals(getServerName())) return;

                Bukkit.getScheduler().runTask(this, () -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        if (state) {
                            if (!staffModeManager.isMod(player)) {
                                staffModeManager.enableStaffMode(player);
                            }
                        } else {
                            if (staffModeManager.isMod(player)) {
                                staffModeManager.disableStaffMode(player);
                            } else {
                                // Important: si on reçoit "false" et qu'on n'est pas mod, on nettoie quand même
                                // au cas où le joueur aurait rejoint avec des items de staff
                                staffModeManager.forceDisableOnJoin(player);
                            }
                        }
                    }
                });
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
        
        // Detailed Startup Message
        getLogger().info("§8§m----------------------------------------");
        getLogger().info("§6§lLifeMod §7- §aSuccessfully Enabled");
        getLogger().info(" ");
        getLogger().info("§e• §fVersion: §b" + getDescription().getVersion());
        getLogger().info("§e• §fPlatform: §aBukkit §7(" + Bukkit.getName() + ")");
        getLogger().info("§e• §fNMS Instance: §d" + Bukkit.getBukkitVersion());
        getLogger().info("§e• §fDatabase: §a" + configConfig.getString("database.type").toUpperCase());
        getLogger().info("§e• §fRedis Sync: " + (configConfig.getBoolean("redis.enabled", false) ? "§aEnabled" : "§cDisabled"));
        getLogger().info("§e• §fStartup Time: §e" + elapsed + "ms");
        getLogger().info(" ");
        getLogger().info("§8§m----------------------------------------");
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
        chatManager = new ChatManager(this);
        databaseManager = new DatabaseManager();
        databaseManager.setupDatabase();
        guiManager = new GuiManager(this);
        noteInputManager = new NoteInputManager(this);
        moderatorAuthService = new ModeratorAuthService(this);
        moderatorSessionManager = new ModeratorSessionManager(configConfig.getInt("modules.moderator-auth.max-attempts", 3));
        reactionManager = new ReactionManager(this);
        
        // Staff System
        staffItemManager = new StaffItemManager(this);
        staffModeManager = new StaffModeManager(this, staffItemManager);
        invseeManager = new InvseeManager();
        staffActionManager = new StaffActionManager();
        antiAltManager = new fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager(this);
        
        // Vanish System
        vanishService = new VanishService(this);
        PacketEvents.getAPI().getEventManager().registerListener(
            new fr.lampalon.lifemod.platform.bukkit.managers.staff.VanishPacketListener(vanishService), 
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
        // pm.registerEvents(new ModItemsInteract(), this); // Deprecated
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffListener(staffModeManager, staffItemManager, staffActionManager), this);
        pm.registerEvents(new fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffPhysicalListener(staffModeManager), this);
        pm.registerEvents(new Staffchatevent(this), this);
        pm.registerEvents(new PluginDisable(), this);
        pm.registerEvents(new PlayerQuit(), this);
        pm.registerEvents(new PlayerTeleportEvent(), this);
        
        CPSListener cpsListener = new CPSListener(cpsMap);
        pm.registerEvents(cpsListener, this);
        PacketEvents.getAPI().getEventManager().registerListener(cpsListener, PacketListenerPriority.NORMAL);
        
        pm.registerEvents(new GuiDetailListener(this), this);
        pm.registerEvents(new ChatAsyncListener(this), this);
        pm.registerEvents(new TicketJoinListener(this, updateChecker), this);
        pm.registerEvents(new ChatListener(), this);
        pm.registerEvents(new ModeratorAuthListener(), this);
        pm.registerEvents(new SanctionListener(), this);
        pm.registerEvents(new ConnectionListener(), this);
        pm.registerEvents(new InvseeListener(this), this);
        pm.registerEvents(new AntiAltListener(this), this);
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
        registerCommand("mod", new ModCmd(this, staffModeManager));
        registerCommand("staff", new ModCmd(this, staffModeManager));
        registerCommand("broadcast", new BroadcastCmd(this));
        registerCommand("bc", new BroadcastCmd(this));
        
        GmCmd gmCmd = new GmCmd(this);
        registerCommand(gmCmd); // registers "gamemode"
        registerCommand("gm", new BukkitCommandAdapter(gmCmd));
        
        registerCommand("ecopen", new EcopenCmd(this));
        registerCommand("vanish", new VanishCmd(this));
        registerCommand("clearinv", new ClearinvCmd(this));
        registerCommand("stafflist", new StafflistCmd());
        registerCommand("staffchat", new StaffchatCmd());
        registerCommand("chatclear", new ChatclearCmd(this));
        registerCommand("heal", new HealCmd(this));
        
        TeleportCmd tpCmd = new TeleportCmd();
        registerCommand(tpCmd); // registers "teleport"
        registerCommand("tp", new BukkitCommandAdapter(tpCmd));
        registerCommand("tphere", new BukkitCommandAdapter(tpCmd));
        
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
    public IVanishService getVanishService() { return vanishService; }
    public DebugManager getDebugManager() { return debugManager; }
    public SpectateManager getSpectateManager() { return spectateManager; }
    public ModeratorSessionManager getModeratorSessionManager() { return moderatorSessionManager; }
    public ModeratorAuthService getModeratorAuthService() { return moderatorAuthService; }
    public ReactionManager getReactionManager() { return reactionManager; }
    public Map<UUID, Deque<Long>> getCpsMap() { return cpsMap; }
    public StaffModeManager getStaffModeManager() { return staffModeManager; }
    public InvseeManager getInvseeManager() { return invseeManager; }
    public fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager getAntiAltManager() { return antiAltManager; }
    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
    public Set<UUID> getModerators() { return moderators; }
    public boolean isFreeze(Player p) { return freezeManager.isPlayerFrozen(p.getUniqueId()); }
    public Map<UUID, Location> getFrozenPlayers() { return freezeManager.getFrozenPlayers(); }
    public void reloadPluginConfig() { configConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml")); }
    public void reloadLangConfig() { langConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml")); }

    public String getServerName() {
        return configConfig.getString("server.name");
    }
}
