package fr.lampalon.lifemod.common.service;

import org.bukkit.inventory.ItemStack;

public interface IItemsAdderService {
    /**
     * Checks if the given ItemStack is an ItemsAdder item.
     */
    boolean isItemsAdderItem(ItemStack itemStack);

    /**
     * Returns the ItemsAdder ID of the given ItemStack, or null if it's not an ItemsAdder item.
     */
    String getItemId(ItemStack itemStack);

    /**
     * Returns an ItemStack for the given ItemsAdder ID, or null if not found.
     */
    ItemStack getItem(String id);
    
    /**
     * Returns true if ItemsAdder is present and enabled.
     */
    boolean isEnabled();
}
