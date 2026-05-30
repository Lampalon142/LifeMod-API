package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
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
    private final DebugManager debug;
    private final Map<String, StaffItem> staffItems = new HashMap<>();
    private final NamespacedKey itemKey;

    public StaffItemManager(LifeMod plugin) {
        this.plugin = plugin;
        this.debug = plugin.getDebugManager();
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

        debug.log("staff", "loadItems: found section with keys=" + section.getKeys(false));

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec == null || !itemSec.getBoolean("enabled", true)) {
                debug.log("staff", "loadItems: skipping " + key + " (disabled or null)");
                continue;
            }

            try {
                fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
                // Build ItemStack
                Material material = Material.valueOf(itemSec.getString("material"));
                int slot = itemSec.getInt("slot", 0);
                String name = lang.formatMessage(itemSec.getString("name", key));
                List<String> loreRaw = itemSec.getStringList("lore");
                String[] lore = loreRaw.stream().map(lang::formatMessage).toArray(String[]::new);

                ItemBuilder builder = new ItemBuilder(material)
                        .setName(name)
                        .setLore(lore);

                // Custom Model Data
                if (itemSec.contains("custom-model-data")) {
                    builder.setCustomModelData(itemSec.getInt("custom-model-data"));
                } else if (itemSec.contains("custom_model_data")) {
                    builder.setCustomModelData(itemSec.getInt("custom_model_data"));
                }

                // Enchants
                if (itemSec.isConfigurationSection("enchantments")) {
                    for (String enchKey : itemSec.getConfigurationSection("enchantments").getKeys(false)) {
                        org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchKey.toLowerCase()));
                        if (enchantment != null) {
                            int level = itemSec.getInt("enchantments." + enchKey);
                            builder.addEnchant(enchantment, level);
                        }
                    }
                }

                ItemStack finalStack = builder.toItemStack();
                ItemMeta meta = finalStack.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, key);
                    finalStack.setItemMeta(meta);
                }

                // Parse Scripted Actions
                Map<String, List<String>> actionScripts = new HashMap<>();

                if (itemSec.isConfigurationSection("actions")) {
                    ConfigurationSection actionsSec = itemSec.getConfigurationSection("actions");
                    for (String clickType : actionsSec.getKeys(false)) {
                        List<String> scripts;
                        if (actionsSec.isList(clickType)) {
                            scripts = actionsSec.getStringList(clickType);
                        } else {
                            scripts = new ArrayList<>();
                            String singleAction = actionsSec.getString(clickType);
                            if (singleAction != null) {
                                // Backward compatibility: if it doesn't have a tag, wrap it correctly
                                if (singleAction.startsWith("/")) {
                                    scripts.add("[PLAYER] " + singleAction.substring(1));
                                } else if (singleAction.toUpperCase().equals(singleAction)) {
                                    scripts.add("[NATIVE] " + singleAction);
                                } else {
                                    scripts.add(singleAction);
                                }
                            }
                        }
                        actionScripts.put(clickType.toUpperCase(), scripts);
                    }
                }

                StaffItem staffItem = new StaffItem(key, finalStack, slot, actionScripts);
                staffItems.put(key, staffItem);
                debug.log("staff", "loadItems: loaded " + key + " slot=" + slot + " mat=" + material + " actions=" + actionScripts);

            } catch (Exception e) {
                plugin.getLogger().severe("Error loading staff item: " + key);
                e.printStackTrace();
            }
        }
    }

    public void giveItems(Player player) {
        debug.log("staff", "giveItems: giving " + staffItems.size() + " items to " + player.getName());
        for (StaffItem item : staffItems.values()) {
            player.getInventory().setItem(item.getSlot(), item.getItemStack());
            debug.log("staff", "giveItems: set slot=" + item.getSlot() + " key=" + item.getKey());
        }
    }

    public StaffItem getStaffItem(ItemStack item) {
        if (item == null) {
            debug.log("staff", "getStaffItem: item is null");
            return null;
        }
        if (item.getType() == Material.AIR) {
            debug.log("staff", "getStaffItem: item is AIR");
            return null;
        }
        if (!item.hasItemMeta()) {
            debug.log("staff", "getStaffItem: item has no ItemMeta, type=" + item.getType());
            return null;
        }

        String key = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (key == null) {
            debug.log("staff", "getStaffItem: no PDC key found for " + item.getType());
            return null;
        }

        StaffItem result = staffItems.get(key);
        debug.log("staff", "getStaffItem: found key=" + key + " result=" + (result == null ? "null" : result.getKey()));
        return result;
    }
}
