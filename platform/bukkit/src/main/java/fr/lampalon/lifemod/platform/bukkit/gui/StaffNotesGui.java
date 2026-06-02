package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.util.List;
import java.util.stream.Collectors;

public class StaffNotesGui extends PagedAbstractGui {

    private final Report report;

    public StaffNotesGui(Player player, Report report) {
        super(player);
        this.report = report;
    }

    @Override
    protected List<Item> getListItems() {
        return report.getStaffNotes().stream()
                .map(note -> new StaffNoteItem(report, note))
                .collect(Collectors.toList());
    }

    @Override
    public Gui buildGui() {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        return PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# # # < A > # # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createBorder())
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        Material mat = Material.valueOf(config.getString("gui.pagination.previous-material", "ARROW"));
                        ItemBuilder builder = new ItemBuilder(mat);
                        builder.setDisplayName(lang.getMessage("gui.pagination.previous"));
                        return builder;
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        Material mat = Material.valueOf(config.getString("gui.pagination.next-material", "ARROW"));
                        ItemBuilder builder = new ItemBuilder(mat);
                        builder.setDisplayName(lang.getMessage("gui.pagination.next"));
                        return builder;
                    }
                })
                .addIngredient('A', new SimpleItem(new ItemBuilder(Material.valueOf(config.getString("gui.notes.add-material", "WRITABLE_BOOK"))).setDisplayName(lang.getMessage("reports.gui.notes.add")), click -> {
                    player.closeInventory();
                    LifeMod.getInstance().getNoteInputManager().startNoteInput(player, report);
                    player.sendMessage(lang.getMessage("reports.gui.notes.prompt"));
                }))
                .setContent(getListItems())
                .build();
    }

    @Override
    protected String getTitle() {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        return lang.getMessage("report.detail.notes.title", "§6Notes: §e") + report.getUuid().toString().substring(0, 8);
    }

    @Override
    protected String getTitleKey() {
        return "report.detail.notes.title";
    }
}
