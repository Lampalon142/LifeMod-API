package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoin implements Listener {
    private final LifeMod plugin;
    private final UpdateChecker updateChecker;
    private final DebugManager debug;

    public PlayerJoin(LifeMod plugin, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.updateChecker = updateChecker;
        this.debug = plugin.getDebugManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);

        // Update Notification
        if (player.hasPermission("lifemod.notify")) {
            updateChecker.checkForUpdates(result -> {
                if (!player.isOnline()) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result == UpdateChecker.UpdateCheckResult.OUT_DATED) {
                        String message = lang.getMessage("system.update.message",
                                "%player%", player.getName(),
                                "%current_version%", updateChecker.getCurrentVersionS(),
                                "%latest_version%", updateChecker.getLatestVersionS());
                        
                        player.sendMessage(message);
                        debug.log("update", "Notified player " + player.getName() + " about update.");
                    } else if (result == UpdateChecker.UpdateCheckResult.NO_RESULT) {
                        player.sendMessage(lang.getMessage("system.update.failed"));
                        debug.log("update", "Failed to retrieve latest version for player " + player.getName());
                    }
                });
            });
        }

        // Staff Mode & Sanction Check
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String ip = player.getAddress().getAddress().getHostAddress();
            PlayerData data = plugin.getDatabaseManager().getDatabaseProvider().getPlayerData(player.getUniqueId());

            if (data != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (data.isInStaffMode()) {
                        plugin.getStaffModeManager().enableStaffMode(player);
                    } else {
                        plugin.getStaffModeManager().forceDisableOnJoin(player);
                    }
                });
            }

            List<PlayerData> alts = plugin.getDatabaseManager().getDatabaseProvider().getAlts(ip);
            boolean evasion = false;
            StringBuilder accounts = new StringBuilder();

            ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
            if (sanctionService == null) return;

            for (PlayerData alt : alts) {
                String color = "&7";
                if (Bukkit.getPlayer(alt.getUuid()) != null) color = "&a";

                Sanction ban = sanctionService.getActiveSanction(alt.getUuid(), alt.getLastName(), SanctionType.BAN).join();
                if (ban != null) {
                    evasion = true;
                    color = "&c";
                }

                accounts.append(color).append(alt.getLastName()).append("&7, ");
            }

            if (evasion) {
                String accountList = accounts.length() > 2 ? accounts.substring(0, accounts.length() - 2) : "";

                String alert = lang.getMessage("alts.evasion-alert",
                        "%player%", player.getName(),
                        "%accounts%", accountList,
                        "%ip%", ip);

                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("lifemod.alts"))
                        .forEach(p -> p.sendMessage(alert));
            }
        });
    }
}
