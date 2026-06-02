package fr.lampalon.lifemod.platform.bukkit.adapter;

import org.bukkit.inventory.ItemStack;

public interface IItemsAdderService {
    boolean isItemsAdderItem(ItemStack itemStack);

    String getItemId(ItemStack itemStack);

    ItemStack getItem(String id);

    boolean isEnabled();
}
