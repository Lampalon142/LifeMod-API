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
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.SanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitLangService;
import fr.lampalon.lifemod.platform.bukkit.commands.engine.CommandRegistry;
import fr.lampalon.lifemod.platform.bukkit.listeners.*;
import fr.lampalon.lifemod.platform.bukkit.managers.*;
import fr.lampalon.lifemod.platform.bukkit.managers.gui.GuiManager;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.*;
import fr.lampalon.lifemod.platform.bukkit.nms.NMSLoader;
import fr.lampalon.lifemod.platform.bukkit.utils.ConfigUpdater;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.UpdateChecker;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

import com.github.retrooper.packetevents.event.PacketListenerPriority;

public class LifeMod extends JavaPlugin {
    private static LifeMod instance;
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
    private ReactionManager reactionManager;
    private StaffItemManager staffItemManager;
    private StaffModeManager staffModeManager;
    private InvseeManager invseeManager;
    private StaffActionManager staffActionManager;
    private IVanishService vanishService;
    private CommandRegistry commandRegistry;
    private fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager antiAltManager;
    private fr.lampalon.lifemod.common.anticheat.AntiCheatService antiCheatService;
    private fr.lampalon.lifemod.common.antivpn.AntiVPNService antiVPNService;
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
        
        BukkitPlatform bukkitPlatform = new BukkitPlatform(this);
        ServiceRegistry.register(ILifePlatform.class, bukkitPlatform);
        ServiceRegistry.register(IConfigurationService.class, new BukkitConfigurationService(configConfig));
        ServiceRegistry.register(ILangService.class, new BukkitLangService(langConfig));
        
        setupRedis();

        PacketEvents.getAPI().init();
        bukkitPlatform.setNmsProvider(NMSLoader.load(getLogger()));
        
        this.webHookUrl = configConfig.getString("modules.discord.webhook-url");
        this.spectateManager = new SpectateManager();
        this.debugManager = new DebugManager(this);
        initializeManagers();
        
        ServiceRegistry.register(ISanctionService.class, new SanctionService(databaseManager.getDatabaseProvider()));

        PacketEvents.getAPI().getEventManager().registerListener(new fr.lampalon.lifemod.platform.bukkit.listeners.FreezePacketListener(this), PacketListenerPriority.NORMAL);
        
        this.commandRegistry = new CommandRegistry(this);
        registerEvents();
        registerCommands();
        setupMetrics();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            databaseManager.getDatabaseProvider().cleanupExpiredSanctions();
        }, 20 * 60L, 20 * 60L);
        
        long elapsed = System.currentTimeMillis() - start;
        printStartupMessage(elapsed);
    }

    private void setupRedis() {
        if (configConfig.getBoolean("redis.enabled", false)) {
            String host = configConfig.getString("redis.host");
            int port = configConfig.getInt("redis.port", 6379);
            String password = configConfig.getString("redis.password");
            IMessagingService redis = new RedisMessagingService(host, port, password);
            ServiceRegistry.register(IMessagingService.class, redis);
            
            // Subscriptions logic would go here if needed
        }
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
        ServiceRegistry.register(DatabaseProvider.class, databaseManager.getDatabaseProvider());
        guiManager = new GuiManager(this);
        noteInputManager = new NoteInputManager(this);
        moderatorAuthService = new ModeratorAuthService(this);
        moderatorSessionManager = new ModeratorSessionManager(configConfig.getInt("modules.moderator-auth.max-attempts", 3));
        reactionManager = new ReactionManager(this);
        
        staffItemManager = new StaffItemManager(this);
        staffModeManager = new StaffModeManager(this, staffItemManager);
        invseeManager = new InvseeManager();
        staffActionManager = new StaffActionManager();
        antiAltManager = new fr.lampalon.lifemod.platform.bukkit.managers.antialt.AntiAltManager(this);
        
        // AntiCheat System
        this.antiCheatService = new fr.lampalon.lifemod.common.anticheat.AntiCheatService(ServiceRegistry.get(ILifePlatform.class));
        this.antiCheatService.init();
        ServiceRegistry.register(fr.lampalon.lifemod.common.anticheat.AntiCheatService.class, this.antiCheatService);

        // AntiVPN System
        this.antiVPNService = new fr.lampalon.lifemod.common.antivpn.AntiVPNService(ServiceRegistry.get(ILifePlatform.class));
        PacketEvents.getAPI().getEventManager().registerListener(
            new fr.lampalon.lifemod.common.antivpn.AntiVPNPacketListener(this.antiVPNService, ServiceRegistry.get(ILifePlatform.class)),
            PacketListenerPriority.LOW
        );
        ServiceRegistry.register(fr.lampalon.lifemod.common.antivpn.AntiVPNService.class, this.antiVPNService);

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
        commandRegistry.scanAndRegisterCommands("fr.lampalon.lifemod.platform.bukkit.commands.impl");
    }

    private void printStartupMessage(long elapsed) {
        getLogger().info("§8§m----------------------------------------");
        getLogger().info("§6§lLifeMod §7- §aSuccessfully Enabled");
        getLogger().info(" ");
        getLogger().info("§e• §fVersion: §b" + getDescription().getVersion());
        getLogger().info("§e• §fPlatform: §aBukkit");
        getLogger().info("§e• §fDatabase: §a" + configConfig.getString("database.type").toUpperCase());
        getLogger().info("§e• §fCommands: §aAuto-Registered (" + commandRegistry.getCommands().size() + ")");
        getLogger().info("§e• §fStartup Time: §e" + elapsed + "ms");
        getLogger().info("§8§m----------------------------------------");
    }

    @Override
    public void onDisable() {
        if (antiCheatService != null) antiCheatService.terminate();
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
