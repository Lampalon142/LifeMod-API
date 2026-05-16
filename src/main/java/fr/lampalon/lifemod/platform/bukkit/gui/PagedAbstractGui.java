package fr.lampalon.lifemod.platform.bukkit.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.util.List;

public abstract class PagedAbstractGui extends AbstractGui {

    public PagedAbstractGui(Player player) {
        super(player);
    }

    protected abstract List<Item> getListItems();

    @Override
    public Gui buildGui() {
        return PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# # # < # > # # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createBorder())
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        ItemBuilder builder = new ItemBuilder(Material.ARROW);
                        builder.setDisplayName(lang.getMessage("gui.pagination.previous"));
                        if (!gui.hasPreviousPage()) builder.addLoreLines(lang.getMessage("gui.pagination.first-page"));
                        return builder;
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        ItemBuilder builder = new ItemBuilder(Material.ARROW);
                        builder.setDisplayName(lang.getMessage("gui.pagination.next"));
                        if (!gui.hasNextPage()) builder.addLoreLines(lang.getMessage("gui.pagination.last-page"));
                        return builder;
                    }
                })
                .setContent(getListItems())
                .build();
    }
}

