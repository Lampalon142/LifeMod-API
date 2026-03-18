package fr.lampalon.lifemod.platform.bukkit.replay.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * UI for controlling replay playback.
 */
public class ReplayGui {

    /**
     * Opens the replay control menu for the moderator.
     *
     * @param player The mod viewing the replay.
     */
    public void open(Player player) {
        Gui gui = Gui.normal()
                .setStructure(
                        "# P S F . . . . .",
                        ". . . . . . . . .")
                .addIngredient('#', new SimpleItem(new ItemBuilder(Material.RED_WOOL).setDisplayName("§cStop")))
                .addIngredient('P', new SimpleItem(new ItemBuilder(Material.YELLOW_WOOL).setDisplayName("§ePause")))
                .addIngredient('S', new SimpleItem(new ItemBuilder(Material.GREEN_WOOL).setDisplayName("§aPlay/Speed x1")))
                .addIngredient('F', new SimpleItem(new ItemBuilder(Material.LIME_WOOL).setDisplayName("§aSpeed x4")))
                .build();

        Window window = Window.single()
                .setGui(gui)
                .setTitle("Replay Controls")
                .build(player);

        window.open();
    }
}