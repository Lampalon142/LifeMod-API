package fr.lampalon.lifemod.platform.bukkit;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlatform implements ILifePlatform {

    private final LifeMod plugin;
    private NMSProvider nmsProvider;

    public BukkitPlatform(LifeMod plugin) {
        this.plugin = plugin;
    }

    public void setNmsProvider(NMSProvider nmsProvider) {
        this.nmsProvider = nmsProvider;
    }

    public NMSProvider getNmsProvider() {
        return nmsProvider;
    }

    @Override
    public void broadcast(String message, String permission) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        String formatted = lang.formatMessage(message);
        if (permission == null) {
            Bukkit.broadcastMessage(formatted);
        } else {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(permission))
                    .forEach(p -> p.sendMessage(formatted));
            Bukkit.getConsoleSender().sendMessage(formatted);
        }
    }

    @Override
    public void kickPlayer(UUID uuid, String reason) {
        runTask(() -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                player.kickPlayer(lang.formatMessage(reason));
            }
        });
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            player.sendMessage(lang.formatMessage(message));
        }
    }

    @Override
    public void runTask(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runTaskAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public String getPlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op != null ? op.getName() : uuid.toString();
    }

    @Override
    public void logInfo(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public String getServerName() {
        return plugin.getConfigConfig().getString("server.name", "Survival");
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public void dispatchCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
