package fr.lampalon.lifemod.platform.bukkit.replay.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class ReplayGui {

    public void open(Player player) {
        ILangService lang = ServiceRegistry.get(ILangService.class);

        Gui gui = Gui.normal()
                .setStructure(
                        "# P S F . . . . .",
                        ". . . . . . . . .")
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.RED_WOOL).setDisplayName(lang.getMessage("replay.gui.stop"))))
                .addIngredient('P', new SimpleItem(new ItemBuilder(Material.YELLOW_WOOL).setDisplayName(lang.getMessage("replay.gui.pause"))))
                .addIngredient('S', new SimpleItem(new ItemBuilder(Material.GREEN_WOOL).setDisplayName(lang.getMessage("replay.gui.play"))))
                .addIngredient('F', new SimpleItem(new ItemBuilder(Material.LIME_WOOL).setDisplayName(lang.getMessage("replay.gui.speed"))))
                .build();

        Window window = Window.single()
                .setGui(gui)
                .setTitle(lang.getMessage("replay.gui.title"))
                .build(player);

        window.open();
    }
}