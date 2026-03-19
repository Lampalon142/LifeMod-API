package fr.lampalon.lifemod.platform.bukkit.replay.gui;

import fr.lampalon.lifemod.platform.bukkit.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ReplayInventoryGui {

    private final Player spectator;
    private final Inventory inventory;

    public ReplayInventoryGui(Player spectator) {
        this.spectator = spectator;
        this.inventory = Bukkit.createInventory(null, 54, "§8Replay Controls");
        setupItems();
    }

    private void setupItems() {
        // Timeline (Top rows)
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§7Timeline").toItemStack());
        }

        // Control Buttons
        inventory.setItem(40, new ItemBuilder(Material.LIME_DYE).setName("§aPlay / Pause").toItemStack());
        inventory.setItem(38, new ItemBuilder(Material.FEATHER).setName("§eSpeed x1.0").toItemStack());
        inventory.setItem(42, new ItemBuilder(Material.ARROW).setName("§bPov Switch").toItemStack());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).setName("§cExit Replay").toItemStack());
    }

    public void open() {
        spectator.openInventory(inventory);
    }
}
