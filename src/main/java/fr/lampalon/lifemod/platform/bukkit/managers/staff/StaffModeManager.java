package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StaffModeManager {

    private final LifeMod plugin;
    private final StaffItemManager itemManager;
    private final Set<UUID> moderators = new HashSet<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final DebugManager debug;

    public StaffModeManager(LifeMod plugin, StaffItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.debug = plugin.getDebugManager();
    }

    public boolean isMod(Player player) {
        return moderators.contains(player.getUniqueId());
    }

    private void setStaffModeState(Player player, boolean state) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            fr.lampalon.lifemod.common.model.PlayerData data = plugin.getDatabaseManager().getDatabaseProvider().getPlayerData(player.getUniqueId());
            if (data != null) {
                data.setInStaffMode(state);
                plugin.getDatabaseManager().getDatabaseProvider().savePlayerData(data);
            }

            // Publish to Redis
            IMessagingService messaging = fr.lampalon.lifemod.common.core.ServiceRegistry.get(IMessagingService.class);
            if (messaging != null) {
                messaging.publish("lifemod:staff", "UPDATE|" + player.getUniqueId() + "|" + state + "|" + plugin.getServerName());
            }
        });
    }

    public void enableStaffMode(Player player) {
        if (isMod(player)) return;

        // 1. Sauvegarder Globalement
        setStaffModeState(player, true);

        // 2. Sauvegarder l'inventaire LOCAL s'il n'est pas déjà sauvegardé
        saveSurvivalInventory(player);

        // 3. Appliquer le mode staff
        applyStaffState(player);
        
        moderators.add(player.getUniqueId());
        
        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("mod.enable")));
        debug.log("mod", player.getName() + " enabled staff mode.");
    }

    public void disableStaffMode(Player player) {
        if (!isMod(player)) return;

        // 1. Sauvegarder Globalement
        setStaffModeState(player, false);

        // 2. Retirer l'état staff
        removeStaffState(player);

        // 3. Restaurer l'inventaire LOCAL
        restoreSurvivalInventory(player);

        moderators.remove(player.getUniqueId());

        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("mod.disable")));
        debug.log("mod", player.getName() + " disabled staff mode.");
    }

    // Applique uniquement les effets visuels et donne les items
    private void applyStaffState(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(Integer.MAX_VALUE, 0));
        
        itemManager.giveItems(player);
        plugin.getVanishService().setVanished(player, true, false);

        plugin.getPacketController().sendTitle(player, plugin.getLangConfig().getString("mod.enable-title"), 
                plugin.getLangConfig().getString("mod.enable-subtitle"), 10, 40, 10);
        plugin.getPacketController().sendActionBar(player, plugin.getLangConfig().getString("mod.actionbar-enabled"));
    }

    private void removeStaffState(Player player) {
        player.getInventory().clear();
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        plugin.getVanishService().setVanished(player, false, false);

        plugin.getPacketController().sendTitle(player, plugin.getLangConfig().getString("mod.disable-title"), 
                plugin.getLangConfig().getString("mod.disable-subtitle"), 10, 40, 10);
        plugin.getPacketController().sendActionBar(player, plugin.getLangConfig().getString("mod.actionbar-disabled"));
    }

    private void saveSurvivalInventory(Player player) {
        String serverName = plugin.getServerName();
        UUID uuid = player.getUniqueId();

        // Check if we have survival items (at least one non-null item that isn't a staff item)
        boolean hasSurvivalItems = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR && itemManager.getStaffItem(item) == null) {
                hasSurvivalItems = true;
                break;
            }
        }

        if (!hasSurvivalItems) {
            debug.log("mod", "Skipping inventory save for " + player.getName() + " because no survival items detected.");
            return;
        }

        // Important: Si on a déjà des items de staff, on NE SAUVEGARDE PAS
        if (hasStaffItems(player)) {
            debug.log("mod", "Skipping inventory save for " + player.getName() + " because staff items detected.");
            return;
        }

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        
        savedInventories.put(uuid, contents);
        savedArmor.put(uuid, armor);

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                byte[] data = fr.lampalon.lifemod.platform.bukkit.utils.InventoryUtil.serializeInventory(contents, armor);
                plugin.getDatabaseManager().getDatabaseProvider().saveRawInventory(uuid, serverName, data);
                debug.log("mod", "Successfully saved survival inventory for " + player.getName() + " on " + serverName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void restoreSurvivalInventory(Player player) {
        String serverName = plugin.getServerName();
        UUID uuid = player.getUniqueId();

        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.get(uuid));
            player.getInventory().setArmorContents(savedArmor.get(uuid));
            savedInventories.remove(uuid);
            savedArmor.remove(uuid);
            
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().getDatabaseProvider().deleteRawInventory(uuid, serverName);
            });
        } else {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                byte[] data = plugin.getDatabaseManager().getDatabaseProvider().getRawInventory(uuid, serverName);
                if (data != null) {
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            fr.lampalon.lifemod.platform.bukkit.utils.InventoryUtil.deserializeInventory(player, data);
                            plugin.getDatabaseManager().getDatabaseProvider().deleteRawInventory(uuid, serverName);
                            debug.log("mod", "Restored " + player.getName() + " inventory from database for server: " + serverName);
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            });
        }
    }

    public void forceDisableOnJoin(Player player) {
        removeStaffState(player);
        restoreSurvivalInventory(player);
        moderators.remove(player.getUniqueId());
    }

    public void cleanupMemoryOnQuit(UUID uuid) {
        moderators.remove(uuid);
        savedInventories.remove(uuid);
        savedArmor.remove(uuid);
    }

    private boolean hasStaffItems(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemManager.getStaffItem(item) != null) return true;
        }
        return false;
    }
}
