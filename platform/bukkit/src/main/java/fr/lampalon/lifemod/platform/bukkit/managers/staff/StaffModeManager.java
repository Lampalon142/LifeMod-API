package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.messaging.IMessagingService;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
import fr.lampalon.lifemod.platform.bukkit.utils.InventoryUtil;
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

    private String cachedGameMode;
    private boolean cachedAllowFlight;
    private boolean cachedFlying;
    private boolean cachedInvulnerable;
    private boolean cachedNightVision;

    public StaffModeManager(LifeMod plugin, StaffItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.debug = plugin.getDebugManager();
        reloadEffectsConfig();
    }

    public void reloadEffectsConfig() {
        IConfigurationService cfg = ServiceRegistry.get(IConfigurationService.class);
        if (cfg == null) return;
        cachedGameMode = cfg.getString("modules.mod-mode.effects.game-mode", "SURVIVAL");
        cachedAllowFlight = cfg.getBoolean("modules.mod-mode.effects.allow-flight", true);
        cachedFlying = cfg.getBoolean("modules.mod-mode.effects.flying", true);
        cachedInvulnerable = cfg.getBoolean("modules.mod-mode.effects.invulnerable", true);
        cachedNightVision = cfg.getBoolean("modules.mod-mode.effects.night-vision", true);
    }

    public boolean isMod(Player player) {
        return moderators.contains(player.getUniqueId());
    }

    public Set<UUID> getStaffPlayers() {
        return new java.util.HashSet<>(moderators);
    }

    private void setStaffModeState(Player player, boolean state) {
        UUID uuid = player.getUniqueId();
        String serverName = plugin.getServerName();
        IMessagingService messaging = ServiceRegistry.get(IMessagingService.class);
        if (messaging != null) {
            messaging.publish("lifemod:staff", "UPDATE|" + uuid + "|" + state + "|" + serverName);
        }

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getDatabaseManager().getDatabaseProvider().getPlayerData(uuid);
            if (data != null) {
                data.setInStaffMode(state);
                plugin.getDatabaseManager().getDatabaseProvider().savePlayerData(data);
                debug.log("mod", "[setStaffModeState] " + player.getName() + " saved isInStaffMode=" + state);
            } else {
                debug.log("mod", "[setStaffModeState] " + player.getName() + " PlayerData is null, cannot save state!");
            }
        });
    }

    public void enableStaffMode(Player player) {
        if (isMod(player)) return;

        ILangService lang = ServiceRegistry.get(ILangService.class);

        // 1. Sauvegarder Globalement
        setStaffModeState(player, true);

        // 2. Sauvegarder l'inventaire LOCAL s'il n'est pas déjà sauvegardé
        saveSurvivalInventory(player);

        // 3. Appliquer le mode staff
        applyStaffState(player);
        
        moderators.add(player.getUniqueId());

        player.sendMessage(lang.getMessage("mod.enable"));
        debug.log("mod", player.getName() + " enabled staff mode.");
    }

    public void disableStaffMode(Player player) {
        if (!isMod(player)) return;

        ILangService lang = ServiceRegistry.get(ILangService.class);

        // 1. Sauvegarder Globalement
        setStaffModeState(player, false);

        // 2. Retirer l'état staff
        removeStaffState(player);

        // 3. Restaurer l'inventaire LOCAL
        restoreSurvivalInventory(player);

        moderators.remove(player.getUniqueId());

        player.sendMessage(lang.getMessage("mod.disable"));
        debug.log("mod", player.getName() + " disabled staff mode.");
    }

    // Applique uniquement les effets visuels et donne les items
    private void applyStaffState(Player player) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        player.getInventory().clear();
        player.setGameMode(GameMode.valueOf(cachedGameMode));
        player.setAllowFlight(cachedAllowFlight);
        player.setFlying(cachedFlying);
        player.setInvulnerable(cachedInvulnerable);
        if (cachedNightVision) {
            player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(Integer.MAX_VALUE, 0));
        }
        
        plugin.getVanishService().setVanished(player, true, false);
        itemManager.giveItems(player);

        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
        if (platform instanceof BukkitPlatform) {
            NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
            if (nms != null) {
                nms.sendTitle(player, lang.getMessage("mod.enable-title"),
                        lang.getMessage("mod.enable-subtitle"), 10, 40, 10);
                nms.sendActionBar(player, lang.getMessage("mod.actionbar-enabled"));
            }
        }
    }

    private void removeStaffState(Player player) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        player.getInventory().clear();
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        plugin.getVanishService().setVanished(player, false, false);

        ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
        if (platform instanceof BukkitPlatform) {
            NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
            if (nms != null) {
                nms.sendTitle(player, lang.getMessage("mod.disable-title"),
                        lang.getMessage("mod.disable-subtitle"), 10, 40, 10);
                nms.sendActionBar(player, lang.getMessage("mod.actionbar-disabled"));
            }
        }
    }

    private void saveSurvivalInventory(Player player) {
        String serverName = plugin.getServerName();
        UUID uuid = player.getUniqueId();

        if (savedInventories.containsKey(uuid)) {
            debug.log("mod", "Skipping inventory save for " + player.getName() + " because already saved this session.");
            return;
        }

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();

        savedInventories.put(uuid, contents);
        savedArmor.put(uuid, armor);

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            byte[] existing = plugin.getDatabaseManager().getDatabaseProvider().getRawInventory(uuid, serverName);
            if (existing != null) {
                debug.log("mod", "Using existing DB inventory for " + player.getName() + " on " + serverName + " (crash/PIN recovery).");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        InventoryUtil.FullInventory full = InventoryUtil.deserializeFullInventory(existing);
                        savedInventories.put(uuid, full.contents());
                        savedArmor.put(uuid, full.armor());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return;
            }
            try {
                byte[] data = InventoryUtil.serializeInventory(contents, armor);
                plugin.getDatabaseManager().getDatabaseProvider().saveRawInventory(uuid, serverName, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void restoreSurvivalInventory(Player player) {
        String serverName = plugin.getServerName();
        UUID uuid = player.getUniqueId();

        if (savedInventories.containsKey(uuid)) {
            debug.log("mod", "[restoreSurvivalInventory] " + player.getName() + " restoring from cache on " + serverName);
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
                debug.log("mod", "[restoreSurvivalInventory] " + player.getName() + " DB lookup on " + serverName + " per-server: " + (data != null ? "FOUND" : "null"));
                if (data == null) {
                    data = plugin.getDatabaseManager().getDatabaseProvider().getRawInventory(uuid);
                    debug.log("mod", "[restoreSurvivalInventory] " + player.getName() + " DB lookup global fallback: " + (data != null ? "FOUND" : "null"));
                }
                if (data != null) {
                    byte[] finalData = data;
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            InventoryUtil.deserializeInventory(player, finalData);
                            plugin.getDatabaseManager().getDatabaseProvider().deleteRawInventory(uuid, serverName);
                            debug.log("mod", "[restoreSurvivalInventory] " + player.getName() + " restored from DB on " + serverName);
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                } else {
                    debug.log("mod", "[restoreSurvivalInventory] " + player.getName() + " nothing to restore on " + serverName + " (no cache, no DB)");
                }
            });
        }
    }

    public void forceDisableOnJoin(Player player) {
        if (moderators.contains(player.getUniqueId())) {
            debug.log("mod", "[forceDisableOnJoin] " + player.getName() + " in moderators set, cleaning up on " + plugin.getServerName());
            removeStaffState(player);
            restoreSurvivalInventory(player);
            moderators.remove(player.getUniqueId());
        } else {
            debug.log("mod", "[forceDisableOnJoin] " + player.getName() + " not in moderators on " + plugin.getServerName() + ", checking DB for orphan save");
            UUID uuid = player.getUniqueId();
            String serverName = plugin.getServerName();
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                byte[] data = plugin.getDatabaseManager().getDatabaseProvider().getRawInventory(uuid, serverName);
                if (data != null) {
                    byte[] finalData = data;
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            // Clean up any lingering staff effects from another server
                            player.setAllowFlight(false);
                            player.setFlying(false);
                            player.setInvulnerable(false);
                            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                            plugin.getVanishService().setVanished(player, false, false);
                            // Then restore inventory
                            InventoryUtil.deserializeInventory(player, finalData);
                            plugin.getDatabaseManager().getDatabaseProvider().deleteRawInventory(uuid, serverName);
                            debug.log("mod", "[forceDisableOnJoin] " + player.getName() + " restored orphan save on " + serverName);
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                } else {
                    debug.log("mod", "[forceDisableOnJoin] " + player.getName() + " no orphan save on " + serverName + ", leaving inventory untouched");
                }
            });
        }
    }

    public void cleanupMemoryOnQuit(UUID uuid) {
        moderators.remove(uuid);
        savedInventories.remove(uuid);
        savedArmor.remove(uuid);
    }
}
