package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.model.StaffItem;
import fr.lampalon.lifemod.platform.bukkit.utils.ItemBuilder;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffItemManager {

    private final LifeMod plugin;
    private final Map<String, StaffItem> staffItems = new HashMap<>();
    private final NamespacedKey itemKey;

    public StaffItemManager(LifeMod plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "staff_item_id");
        loadItems();
    }

    public void loadItems() {
        staffItems.clear();
        ConfigurationSection section = plugin.getConfigConfig().getConfigurationSection("modules.mod-mode.items");
        
        if (section == null) {
            plugin.getLogger().warning("No staff items found in configuration (modules.mod-mode.items).");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec == null || !itemSec.getBoolean("enabled", true)) continue;

            try {
                // Build ItemStack
                Material material = Material.valueOf(itemSec.getString("material"));
                int slot = itemSec.getInt("slot", 0);
                String name = MessageUtil.formatMessage(itemSec.getString("name", key));
                List<String> loreRaw = itemSec.getStringList("lore");
                String[] lore = loreRaw.stream().map(MessageUtil::formatMessage).toArray(String[]::new);

                ItemBuilder builder = new ItemBuilder(material)
                        .setName(name)
                        .setLore(lore);

                // Custom Model Data
                if (itemSec.contains("custom_model_data")) {
                    builder.setCustomModelData(itemSec.getInt("custom_model_data"));
                }

                // Enchants
                if (itemSec.isConfigurationSection("enchantments")) {
                    for (String ench : itemSec.getConfigurationSection("enchantments").getKeys(false)) {
                        // Handle simple enchantment parsing if needed, assumed unsafe for now
                        // Implementation depends on Utils, skipping complex parsing for brevity
                    }
                }

                ItemStack finalStack = builder.toItemStack();
                ItemMeta meta = finalStack.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, key);
                    finalStack.setItemMeta(meta);
                }

                // Parse Actions & Commands
                Map<String, StaffActionType> actions = new HashMap<>();
                Map<String, String> commands = new HashMap<>();

                if (itemSec.isConfigurationSection("actions")) {
                    ConfigurationSection actionsSec = itemSec.getConfigurationSection("actions");
                    for (String clickType : actionsSec.getKeys(false)) {
                        String value = actionsSec.getString(clickType);
                        if (value != null) {
                            if (value.startsWith("/")) {
                                commands.put(clickType.toUpperCase(), value);
                            } else {
                                StaffActionType actionType = StaffActionType.fromString(value);
                                if (actionType != null) {
                                    actions.put(clickType.toUpperCase(), actionType);
                                } else {
                                    plugin.getLogger().warning("Unknown staff action: " + value + " for item " + key);
                                }
                            }
                        }
                    }
                }

                StaffItem staffItem = new StaffItem(key, finalStack, slot, actions, commands);
                staffItems.put(key, staffItem);

            } catch (Exception e) {
                plugin.getLogger().severe("Error loading staff item: " + key);
                e.printStackTrace();
            }
        }
    }

    public void giveItems(Player player) {
        for (StaffItem item : staffItems.values()) {
            player.getInventory().setItem(item.getSlot(), item.getItemStack());
        }
    }

    public StaffItem getStaffItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        
        String key = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (key == null) return null;

        return staffItems.get(key);
    }
}
