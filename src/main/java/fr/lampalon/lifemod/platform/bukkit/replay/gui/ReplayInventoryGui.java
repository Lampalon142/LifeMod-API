package fr.lampalon.lifemod.platform.bukkit.replay.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ReplayInventoryGui {

    private final Player spectator;
    private final Inventory inventory;

    public ReplayInventoryGui(Player spectator) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        this.spectator = spectator;
        this.inventory = Bukkit.createInventory(null, 54, lang.getMessage("replay.inventory.title"));
        setupItems(lang);
    }

    private void setupItems(ILangService lang) {
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(lang.getMessage("replay.inventory.timeline")).toItemStack());
        }

        inventory.setItem(40, new ItemBuilder(Material.LIME_DYE).setName(lang.getMessage("replay.inventory.play-pause")).toItemStack());
        inventory.setItem(38, new ItemBuilder(Material.FEATHER).setName(lang.getMessage("replay.inventory.speed")).toItemStack());
        inventory.setItem(42, new ItemBuilder(Material.ARROW).setName(lang.getMessage("replay.inventory.pov-switch")).toItemStack());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).setName(lang.getMessage("replay.inventory.exit")).toItemStack());
    }

    public void open() {
        spectator.openInventory(inventory);
    }
}
