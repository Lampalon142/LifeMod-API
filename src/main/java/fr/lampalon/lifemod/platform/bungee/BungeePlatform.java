package fr.lampalon.lifemod.platform.bungee;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.UUID;

public class BungeePlatform implements ILifePlatform {

    private final Plugin plugin;

    public BungeePlatform(Plugin plugin) {
        this.plugin = plugin;
    }

    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public void broadcast(String message, String permission) {
        TextComponent component = new TextComponent(format(message));
        if (permission == null) {
            ProxyServer.getInstance().broadcast(component);
        } else {
            ProxyServer.getInstance().getPlayers().stream()
                    .filter(p -> p.hasPermission(permission))
                    .forEach(p -> p.sendMessage(component));
            ProxyServer.getInstance().getConsole().sendMessage(component);
        }
    }

    @Override
    public void kickPlayer(UUID uuid, String reason) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            player.disconnect(new TextComponent(format(reason)));
        }
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        if (player != null) {
            player.sendMessage(new TextComponent(format(message)));
        }
    }

    @Override
    public void runTask(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void runTaskAsync(Runnable runnable) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, runnable);
    }

    @Override
    public String getPlayerName(UUID uuid) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid);
        return player != null ? player.getName() : uuid.toString();
    }

    @Override
    public void logInfo(String message) {
        ProxyServer.getInstance().getLogger().info(message);
    }

    @Override
    public String getServerName() {
        return "BungeeCord";
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        return ProxyServer.getInstance().getPlayer(uuid) != null;
    }

    @Override
    public void dispatchCommand(String command) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
    }

    @Override
    public NMSProvider getNmsProvider() {
        return null;
    }
}
