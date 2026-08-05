package fr.lampalon.lifemod.api.staff;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public interface IStaffItemsService {

    Collection<StaffItem> getItems();

    StaffItem getItem(String key);

    StaffItem getItemFromItemStack(ItemStack item);

    void giveItems(Player player);

    void reload();
}