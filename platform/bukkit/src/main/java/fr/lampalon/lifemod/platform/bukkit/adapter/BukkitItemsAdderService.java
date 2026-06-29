package fr.lampalon.lifemod.platform.bukkit.adapter;

import fr.lampalon.lifemod.platform.bukkit.adapter.IItemsAdderService;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.logging.Logger;

public class BukkitItemsAdderService implements IItemsAdderService {
    private static final Logger LOGGER = Logger.getLogger(BukkitItemsAdderService.class.getName());

    private boolean enabled = false;
    private Class<?> customStackClass;
    private Method byItemStackMethod;
    private Method getInstanceMethod;
    private Method getNamespacedIDMethod;
    private Method getItemStackMethod;

    public BukkitItemsAdderService() {
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            try {
                customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                byItemStackMethod = customStackClass.getMethod("byItemStack", ItemStack.class);
                getInstanceMethod = customStackClass.getMethod("getInstance", String.class);
                getNamespacedIDMethod = customStackClass.getMethod("getNamespacedID");
                getItemStackMethod = customStackClass.getMethod("getItemStack");
                enabled = true;
            } catch (Exception e) {
                LOGGER.fine("ItemsAdder not available: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isItemsAdderItem(ItemStack itemStack) {
        if (!enabled || itemStack == null) return false;
        try {
            Object customStack = byItemStackMethod.invoke(null, itemStack);
            return customStack != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getItemId(ItemStack itemStack) {
        if (!enabled || itemStack == null) return null;
        try {
            Object customStack = byItemStackMethod.invoke(null, itemStack);
            if (customStack != null) {
                return (String) getNamespacedIDMethod.invoke(customStack);
            }
        } catch (Exception e) {
            LOGGER.fine("Failed to get ItemsAdder id: " + e.getMessage());
        }
        return null;
    }

    @Override
    public ItemStack getItem(String id) {
        if (!enabled || id == null) return null;
        try {
            Object customStack = getInstanceMethod.invoke(null, id);
            if (customStack != null) {
                return (ItemStack) getItemStackMethod.invoke(customStack);
            }
        } catch (Exception e) {
            LOGGER.fine("Failed to get ItemsAdder item: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
