package fr.lampalon.lifemod.platform.bukkit.replay.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class ReplayGui {

    public void open(Player player) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        ILangService lang = ServiceRegistry.get(ILangService.class);

        Material stopMat = Material.valueOf(config.getString("modules.replay.gui.controls.stop-material", "RED_WOOL"));
        Material pauseMat = Material.valueOf(config.getString("modules.replay.gui.controls.pause-material", "YELLOW_WOOL"));
        Material playMat = Material.valueOf(config.getString("modules.replay.gui.controls.play-material", "GREEN_WOOL"));
        Material speedMat = Material.valueOf(config.getString("modules.replay.gui.controls.speed-material", "LIME_WOOL"));

        Gui gui = Gui.normal()
                .setStructure(
                        "# P S F . . . . .",
                        ". . . . . . . . .")
                .addIngredient('#', new SimpleItem(new ItemBuilder(stopMat).setDisplayName(lang.getMessage("replay.gui.stop"))))
                .addIngredient('P', new SimpleItem(new ItemBuilder(pauseMat).setDisplayName(lang.getMessage("replay.gui.pause"))))
                .addIngredient('S', new SimpleItem(new ItemBuilder(playMat).setDisplayName(lang.getMessage("replay.gui.play"))))
                .addIngredient('F', new SimpleItem(new ItemBuilder(speedMat).setDisplayName(lang.getMessage("replay.gui.speed"))))
                .build();

        Window window = Window.single()
                .setGui(gui)
                .setTitle(lang.getMessage("replay.gui.title"))
                .build(player);

        window.open();
    }
}