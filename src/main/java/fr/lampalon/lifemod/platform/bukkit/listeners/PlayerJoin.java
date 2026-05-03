package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.ReportStatus;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("lifemod.notify")) {
            updateChecker.checkForUpdates(result -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (updateChecker.getLatestVersionS() == null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLangConfig().getString("system.update.failed")));
                        debug.log("update", "Failed to retrieve latest version for player " + player.getName());
                    } else if (result == UpdateChecker.UpdateCheckResult.OUT_DATED) {
                        String rawMessage = plugin.getLangConfig().getString("system.update.message");
                        if (rawMessage == null) {
                            rawMessage = "&dHello %player%\n" +
                                    "&bLifeMod plugin has an available update!\n" +
                                    "&eYour version: &c%current_version%\n" +
                                    "&eNew version: &a%latest_version%\n" +
                                    "&eLink: &bhttps://www.spigotmc.org/resources/1-8-1-20-lifemod-moderation-plugin.112381/\n" +
                                    "&r\n" +
                                    "&e===========================";
                        }
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                rawMessage.replace("%player%", player.getName())
                                        .replace("%current_version%", updateChecker.getCurrentVersionS())
                                        .replace("%latest_version%", updateChecker.getLatestVersionS())));
                        debug.log("update", "Notified player " + player.getName() + " about update.");
                    } else if (result == UpdateChecker.UpdateCheckResult.UP_TO_DATE) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLangConfig().getString("system.update.up-to-date")));
                        debug.log("update", "Player " + player.getName() + " has up-to-date plugin.");
                    } else if (result == UpdateChecker.UpdateCheckResult.UNRELEASED) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLangConfig().getString("system.update.unreleased")));
                        debug.log("update", "Player " + player.getName() + " is using unreleased version.");
                    } else if (result == UpdateChecker.UpdateCheckResult.NO_RESULT) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLangConfig().getString("system.update.error")));
                        debug.log("update", "Unable to check updates for player " + player.getName());
                    }
                });
            });
        }

        // Ban Evasion Check
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String ip = player.getAddress().getAddress().getHostAddress();
            fr.lampalon.lifemod.common.model.PlayerData data = plugin.getDatabaseManager().getDatabaseProvider().getPlayerData(player.getUniqueId());
            
            if (data != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (data.isInStaffMode()) {
                        // S'il doit être staff, on applique juste l'état visuel et les items
                        // Le manager s'occupera de ne pas écraser l'inventaire de survie s'il existe déjà
                        plugin.getStaffModeManager().enableStaffMode(player);
                    } else {
                        // S'il ne doit pas être staff, on force le nettoyage
                        plugin.getStaffModeManager().forceDisableOnJoin(player);
                    }
                });
            }

            List<fr.lampalon.lifemod.common.model.PlayerData> alts = plugin.getDatabaseManager().getDatabaseProvider().getAlts(ip);
            boolean evasion = false;
            StringBuilder accounts = new StringBuilder();

            fr.lampalon.lifemod.common.service.ISanctionService sanctionService = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ISanctionService.class);

            for (fr.lampalon.lifemod.common.model.PlayerData alt : alts) {
                String color = "&7";
                if (Bukkit.getPlayer(alt.getUuid()) != null) color = "&a";

                fr.lampalon.lifemod.common.model.Sanction ban = sanctionService.getActiveSanction(alt.getUuid(), alt.getLastName(), fr.lampalon.lifemod.common.model.SanctionType.BAN).join();
                if (ban != null) {
                    evasion = true;
                    color = "&c";
                }
                
                accounts.append(color).append(alt.getLastName()).append("&7, ");
            }

            if (evasion) {
                String accountList = accounts.length() > 2 ? accounts.substring(0, accounts.length() - 2) : "";
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                
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


