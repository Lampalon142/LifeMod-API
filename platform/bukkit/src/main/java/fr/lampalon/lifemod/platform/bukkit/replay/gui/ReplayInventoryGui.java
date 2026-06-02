package fr.lampalon.lifemod.platform.bukkit.replay.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
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
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        ILangService lang = ServiceRegistry.get(ILangService.class);
        this.spectator = spectator;
        this.inventory = Bukkit.createInventory(null, 54, lang.getMessage("replay.inventory.title"));
        setupItems(config, lang);
    }

    private void setupItems(IConfigurationService config, ILangService lang) {
        Material timelineMat = Material.valueOf(config.getString("modules.replay.gui.inventory.timeline-material", "GRAY_STAINED_GLASS_PANE"));
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, new ItemBuilder(timelineMat).setName(lang.getMessage("replay.inventory.timeline")).toItemStack());
        }

        Material playPauseMat = Material.valueOf(config.getString("modules.replay.gui.inventory.play-pause.material", "LIME_DYE"));
        int playPauseSlot = config.getInt("modules.replay.gui.inventory.play-pause.slot", 40);
        inventory.setItem(playPauseSlot, new ItemBuilder(playPauseMat).setName(lang.getMessage("replay.inventory.play-pause")).toItemStack());

        Material speedMat = Material.valueOf(config.getString("modules.replay.gui.inventory.speed.material", "FEATHER"));
        int speedSlot = config.getInt("modules.replay.gui.inventory.speed.slot", 38);
        inventory.setItem(speedSlot, new ItemBuilder(speedMat).setName(lang.getMessage("replay.inventory.speed")).toItemStack());

        Material povMat = Material.valueOf(config.getString("modules.replay.gui.inventory.pov-switch.material", "ARROW"));
        int povSlot = config.getInt("modules.replay.gui.inventory.pov-switch.slot", 42);
        inventory.setItem(povSlot, new ItemBuilder(povMat).setName(lang.getMessage("replay.inventory.pov-switch")).toItemStack());

        Material exitMat = Material.valueOf(config.getString("modules.replay.gui.inventory.exit.material", "BARRIER"));
        int exitSlot = config.getInt("modules.replay.gui.inventory.exit.slot", 49);
        inventory.setItem(exitSlot, new ItemBuilder(exitMat).setName(lang.getMessage("replay.inventory.exit")).toItemStack());
    }

    public void open() {
        spectator.openInventory(inventory);
    }
}
