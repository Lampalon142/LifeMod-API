package fr.lampalon.lifemod.platform.bukkit

import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseProvider;;

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
import fr.lampalon.lifemod.integration.nms.PacketController;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitConfigurationService;
import fr.lampalon.lifemod.platform.bukkit.adapter.BukkitLangService;
import fr.lampalon.lifemod.platform.bukkit.commands.*;
import fr.lampalon.lifemod.platform.bukkit.commands.adapter.BukkitCommandAdapter;
import fr.lampalon.lifemod.platform.bukkit.listeners.*;
import fr.lampalon.lifemod.platform.bukkit.managers.*;
import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseManager;
import fr.lampalon.lifemod.platform.bukkit.managers.gui.GuiManager;
import fr.lampalon.lifemod.platform.bukkit.utils.ConfigUpdater;
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
        
        ServiceRegistry.register(IConfigurationService.class, new BukkitConfigurationService(configConfig));
        ServiceRegistry.register(ILangService.class, new BukkitLangService(langConfig));
        
        if (configConfig.getBoolean("redis.enabled", false)) {
            String host = configConfig.getString("redis.host", "localhost");
            int port = configConfig.getInt("redis.port", 6379);
            String password = configConfig.getString("redis.password", "");
            ServiceRegistry.register(IMessagingService.class, new RedisMessagingService(host, port, password));
        }

        PacketEvents.getAPI().init();
        
        this.webHookUrl = getConfig().getString("discord.webhookurl");
        this.spectateManager = new SpectateManager();
        this.debugManager = new DebugManager(this);
        initializeManagers();
        
        ServiceRegistry.register(ISanctionService.class, new SanctionService(databaseManager.getDatabaseProvider()));

        this.packetController = new PacketController(this);
        PacketEvents.getAPI().getEventManager().registerListener(this.packetController, PacketListenerPriority.NORMAL);
        
        registerEvents();
        registerCommands();
        setupMetrics();
        
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
        databaseManager = new DatabaseManager(this);
        databaseManager.setupDatabase();
        guiManager = new GuiManager(this);
        noteInputManager = new NoteInputManager(this);
        moderatorAuthService = new ModeratorAuthService(this);
        moderatorSessionManager = new ModeratorSessionManager(configConfig.getInt("moderator-login.max-attempts", 3));
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
        pm.registerEvents(new FreezeGui(this), this);
        pm.registerEvents(new PlayerTeleportEvent(), this);
        pm.registerEvents(new CPSListener(cpsMap), this);
        pm.registerEvents(new GuiDetailListener(this), this);
        pm.registerEvents(new ChatAsyncListener(this), this);
        pm.registerEvents(new TicketJoinListener(this, updateChecker), this);
        pm.registerEvents(new ChatListener(), this);
        pm.registerEvents(new ModeratorAuthListener(), this);
        pm.registerEvents(new SanctionListener(), this);
        pm.registerEvents(new ConnectionListener(), this);
        if (langConfig.getBoolean("general.update.enabled")) {
            pm.registerEvents(new PlayerJoin(this, updateChecker), this);
        }
    }

    private void registerCommands() {
        registerCommand(new FlyCmd(this));
        registerCommand(new BanCmd());
        registerCommand(new MuteCmd());
        registerCommand(new WarnCmd());
        registerCommand(new NoteCmd());
        registerCommand(new UnbanCmd());
        registerCommand(new UnmuteCmd());
        registerCommand(new HistoryCmd());
        registerCommand(new AltsCmd());
        
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
        if (getConfig().getBoolean("commands-enabled." + commandName, true)) {
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
    public GuiManager getGuiManager() { return guiManager; }
    public NoteInputManager getNoteInputManager() { return noteInputManager; }
    public PacketController getPacketController() { return packetController; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public VanishedManager getPlayerManager() { return playerManager; }
    public boolean isFreeze(Player p) { return freezeManager.isPlayerFrozen(p.getUniqueId()); }
    public Map<UUID, Location> getFrozenPlayers() { return freezeManager.getFrozenPlayers(); }
    public void reloadPluginConfig() { configConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml")); }
    public void reloadLangConfig() { langConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml")); }
}

