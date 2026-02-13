package fr.lampalon.lifemod.platform.bukkit.managers.staff.model;

import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class StaffItem {
    private final String key;
    private final ItemStack itemStack;
    private final int slot;
    private final Map<String, StaffActionType> actions; // ex: "RIGHT_CLICK" -> JUMP
    private final Map<String, String> commands; // ex: "LEFT_CLICK" -> "ban %player%"

    public StaffItem(String key, ItemStack itemStack, int slot, Map<String, StaffActionType> actions, Map<String, String> commands) {
        this.key = key;
        this.itemStack = itemStack;
        this.slot = slot;
        this.actions = actions;
        this.commands = commands;
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

    public StaffActionType getAction(String clickType) {
        return actions.get(clickType);
    }

    public String getCommand(String clickType) {
        return commands.get(clickType);
    }
}
