package fr.lampalon.lifemod.platform.bukkit.managers.staff.model;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class StaffItem {
    private final String key;
    private final ItemStack itemStack;
    private final int slot;
    private final Map<String, List<String>> actionScripts;

    public StaffItem(String key, ItemStack itemStack, int slot, Map<String, List<String>> actionScripts) {
        this.key = key;
        this.itemStack = itemStack;
        this.slot = slot;
        this.actionScripts = actionScripts;
    }

    public String getKey() {
        return key;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getSlot() {
        return slot;
    }

    public List<String> getScripts(String clickType) {
        return actionScripts.get(clickType);
    }
}
