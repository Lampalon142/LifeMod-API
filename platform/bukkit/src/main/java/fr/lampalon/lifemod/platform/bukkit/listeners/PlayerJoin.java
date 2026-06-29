package fr.lampalon.lifemod.platform.bukkit.listeners;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.InventoryUtil;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerJoin implements Listener {
    private final LifeMod plugin;
    private final UpdateChecker updateChecker;
    private final DebugManager debug;

    public PlayerJoin(LifeMod plugin, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.updateChecker = updateChecker;
        this.debug = plugin.getDebugManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            byte[] invData = plugin.getDatabaseManager().getDatabaseProvider()
                .getRawInventory(player.getUniqueId(), plugin.getServerName());
            if (invData != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        InventoryUtil.deserializeInventory(player, invData);
                        player.updateInventory();
                        plugin.getDatabaseManager().getDatabaseProvider()
                            .deleteRawInventory(player.getUniqueId(), plugin.getServerName());
                        debug.log("invsee", "Restored offline-edited inventory for " + player.getName());
                    } catch (Exception e) {
                        debug.log("invsee", "Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
                    }
                });
            }
        });

        if (player.hasPermission("lifemod.notify") && lang.getBoolean("system.update.enabled")) {
            updateChecker.checkForUpdates(result -> {
                if (!player.isOnline()) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result == UpdateChecker.UpdateCheckResult.UP_TO_DATE){
                        String message = lang.getMessage("system.update.up-to-date",
                                "%player%", player.getName(),
                                "%current_version%", updateChecker.getCurrentVersionS(),
                                "%latest_version%", updateChecker.getLatestVersionS());

                        player.sendMessage(message);
                        debug.log("update", "Notified player " + player.getName() + " about update.");
                    } else if (result == UpdateChecker.UpdateCheckResult.OUT_DATED) {
                        String message = lang.getMessage("system.update.message",
                                "%player%", player.getName(),
                                "%current_version%", updateChecker.getCurrentVersionS(),
                                "%latest_version%", updateChecker.getLatestVersionS());
                        
                        player.sendMessage(message);
                        debug.log("update", "Notified player " + player.getName() + " about update.");
                    } else if (result == UpdateChecker.UpdateCheckResult.UNRELEASED) {
                        String message = lang.getMessage("system.update.unreleased",
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

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!player.isOnline()) return;
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
            PlayerData data = plugin.getDatabaseManager().getDatabaseProvider().getPlayerData(player.getUniqueId());

            if (data != null) {
                boolean staffMode = data.isInStaffMode();
                debug.log("mod", "[PlayerJoin] " + player.getName() + " isInStaffMode=" + staffMode + " from DB on " + plugin.getServerName());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (staffMode) {
                        debug.log("mod", "[PlayerJoin] " + player.getName() + " calling enableStaffMode on " + plugin.getServerName());
                        plugin.getStaffModeManager().enableStaffMode(player);
                    } else {
                        debug.log("mod", "[PlayerJoin] " + player.getName() + " calling forceDisableOnJoin on " + plugin.getServerName());
                        plugin.getStaffModeManager().forceDisableOnJoin(player);
                    }
                });
            } else {
                debug.log("mod", "[PlayerJoin] " + player.getName() + " PlayerData is null on " + plugin.getServerName());
            }

            List<PlayerData> alts = plugin.getDatabaseManager().getDatabaseProvider().getAlts(ip);

            ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);
            if (sanctionService == null) return;

            List<CompletableFuture<Sanction>> futures = alts.stream()
                    .map(alt -> sanctionService.getActiveSanction(alt.getUuid(), alt.getLastName(), SanctionType.BAN))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
                boolean evasion = false;
                StringBuilder accounts = new StringBuilder();

                for (int i = 0; i < alts.size(); i++) {
                    PlayerData alt = alts.get(i);
                    String color = lang.getMessage("alts.color.offline");
                    if (Bukkit.getPlayer(alt.getUuid()) != null) color = lang.getMessage("alts.color.online");

                    Sanction ban = futures.get(i).join();
                    if (ban != null) {
                        evasion = true;
                        color = lang.getMessage("alts.color.banned");
                    }

                    accounts.append(color).append(alt.getLastName()).append(lang.getMessage("alts.color.offline")).append(", ");
                }

                if (evasion) {
                    String accountList = accounts.length() > 2 ? accounts.substring(0, accounts.length() - 2) : "";

                    String alert = lang.getMessage("gui.alts.evasion-alert",
                            "%player%", player.getName(),
                            "%accounts%", accountList,
                            "%ip%", ip);

                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.hasPermission("lifemod.alts"))
                            .forEach(p -> p.sendMessage(alert));
                }
            });
        });
    }

}
