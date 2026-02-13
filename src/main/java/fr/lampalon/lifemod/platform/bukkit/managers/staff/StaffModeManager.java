package fr.lampalon.lifemod.platform.bukkit.managers.staff;

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

    public void enableStaffMode(Player player) {
        if (isMod(player)) return;

        // Save Inventory
        saveInventory(player);

        // Clear & Setup
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL); // Or Adventure, depending on config
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.addPotionEffect(PotionEffectType.NIGHT_VISION.createEffect(Integer.MAX_VALUE, 0)); // No particle

        // Give Items
        itemManager.giveItems(player);

        // Vanish (Default to true when entering mod mode)
        plugin.getVanishService().setVanished(player, true, false);

        moderators.add(player.getUniqueId());
        plugin.getModerators().add(player.getUniqueId()); // Keep legacy compatibility if needed
        
        // NMS Visual Feedback
        plugin.getPacketController().sendTitle(player, plugin.getLangConfig().getString("mod.enable-title"), 
                plugin.getLangConfig().getString("mod.enable-subtitle"), 10, 40, 10);
        plugin.getPacketController().sendActionBar(player, plugin.getLangConfig().getString("mod.actionbar-enabled"));

        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("mod.enable")));
        debug.log("mod", player.getName() + " enabled staff mode (New System).");
    }

    public void disableStaffMode(Player player) {
        if (!isMod(player)) return;

        player.getInventory().clear();
        
        // Restore Inventory
        restoreInventory(player);

        // Reset Attributes
        player.setAllowFlight(false); // Should check permission or previous state ideally, but safety first
        player.setFlying(false);
        player.setInvulnerable(false);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        
        // Unvanish
        plugin.getVanishService().setVanished(player, false, false);

        moderators.remove(player.getUniqueId());
        plugin.getModerators().remove(player.getUniqueId()); // Legacy compatibility

        // NMS Visual Feedback
        plugin.getPacketController().sendTitle(player, plugin.getLangConfig().getString("mod.disable-title"), 
                plugin.getLangConfig().getString("mod.disable-subtitle"), 10, 40, 10);
        plugin.getPacketController().sendActionBar(player, plugin.getLangConfig().getString("mod.actionbar-disabled"));

        player.sendMessage(MessageUtil.formatMessage(plugin.getLangConfig().getString("mod.disable")));
        debug.log("mod", player.getName() + " disabled staff mode (New System).");
    }

    private void saveInventory(Player player) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
    }

    private void restoreInventory(Player player) {
        if (savedInventories.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(savedInventories.get(player.getUniqueId()));
            savedInventories.remove(player.getUniqueId());
        }
        if (savedArmor.containsKey(player.getUniqueId())) {
            player.getInventory().setArmorContents(savedArmor.get(player.getUniqueId()));
            savedArmor.remove(player.getUniqueId());
        }
    }
}
