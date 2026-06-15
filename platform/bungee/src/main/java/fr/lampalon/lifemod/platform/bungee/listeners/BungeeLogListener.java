package fr.lampalon.lifemod.platform.bungee.listeners;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILogService;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BungeeLogListener implements Listener {

    private static final Set<String> IGNORED_CMDS = new HashSet<>(Arrays.asList(
        "shop", "warp", "spawn", "tpa", "tpahere",
        "bal", "balance", "pay", "msg", "r", "reply"
    ));

    private final ILogService logService;
    private final IConfigurationService config;

    public BungeeLogListener() {
        this.logService = ServiceRegistry.get(ILogService.class);
        this.config = ServiceRegistry.get(IConfigurationService.class);
    }

    private boolean enabled(String path) {
        return config.getBoolean("logs." + path, true);
    }

    private String serverName() {
        return config.getString("server.name", "unknown");
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(LoginEvent event) {
        if (!enabled("log-connection")) return;
        if (event.isCancelled()) {
            UUID uuid = event.getConnection().getUniqueId();
            String name = event.getConnection().getName();
            logService.log(LogEntry.builder()
                .type(LogType.LOGIN_FAILED_OTHER).playerUuid(uuid).playerName(name)
                .actionData("{\"r\":\"" + jsonEscape(event.getCancelReasonComponents().toString()) + "\"}")
                .serverName(serverName()).now().build());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPostLogin(PostLoginEvent event) {
        if (!enabled("log-connection")) return;
        ProxiedPlayer p = event.getPlayer();
        logService.log(LogEntry.builder()
            .type(LogType.LOGIN_SUCCESS).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"ip\":\"" + p.getAddress() + "\"}")
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDisconnect(PlayerDisconnectEvent event) {
        if (!enabled("log-connection")) return;
        ProxiedPlayer p = event.getPlayer();
        logService.log(LogEntry.builder()
            .type(LogType.QUIT).playerUuid(p.getUniqueId()).playerName(p.getName())
            .serverName(serverName()).now().build());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) return;
        ProxiedPlayer p = (ProxiedPlayer) event.getSender();
        String msg = event.getMessage();

        if (msg.startsWith("/")) {
            if (!enabled("log-commands")) return;
            String base = msg.substring(1).split(" ")[0].toLowerCase();
            if (IGNORED_CMDS.contains(base)) return;

            logService.log(LogEntry.builder()
                .type(LogType.COMMAND_EXECUTED).playerUuid(p.getUniqueId()).playerName(p.getName())
                .actionData("{\"c\":\"" + jsonEscape(msg) + "\"}")
                .serverName(serverName()).now().build());
        } else {
            if (!enabled("log-chat")) return;
            logService.log(LogEntry.builder()
                .type(LogType.CHAT_MESSAGE).playerUuid(p.getUniqueId()).playerName(p.getName())
                .actionData("{\"m\":\"" + jsonEscape(msg) + "\"}")
                .serverName(serverName()).now().build());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerSwitch(ServerSwitchEvent event) {
        if (!enabled("log-teleport")) return;
        ProxiedPlayer p = event.getPlayer();
        String from = p.getServer() != null ? p.getServer().getInfo().getName() : "none";
        String to = event.getPlayer().getServer().getInfo().getName();
        logService.log(LogEntry.builder()
            .type(LogType.WORLD_CHANGE).playerUuid(p.getUniqueId()).playerName(p.getName())
            .actionData("{\"f\":\"" + from + "\",\"t\":\"" + to + "\"}")
            .serverName(serverName()).now().build());
    }
}
