package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Report;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;

public class ReportDetailGui extends AbstractGui {

    private final Report report;

    public ReportDetailGui(Player player, Report report) {
        super(player);
        this.report = report;
    }

    @Override
    public Gui buildGui() {
        return Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# R . T . . . . #",
                        "# . . . . . . . #",
                        "# . I . . . . . #",
                        "# . . . . . . . #",
                        "# # # # B # # # #")
                .addIngredient('#', createBorder())
                .addIngredient('R', new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("§eReporter: §f" + report.getReporterUuid())))
                .addIngredient('T', new SimpleItem(new ItemBuilder(Material.PLAYER_HEAD).setDisplayName("§cTarget: §f" + report.getTargetUuid())))
                .addIngredient('I', createActionItem(Material.BOOK, "report.detail.info.name", "report.detail.info.lore", 
                        "%reason%", report.getReason(),
                        "%server%", report.getServerName()))
                .addIngredient('B', new SimpleItem(new ItemBuilder(Material.ARROW).setDisplayName("§7Retour"), click -> {
                    // Exemple de retour
                    player.closeInventory();
                }))
                .build();
    }

    @Override
    protected String getTitleKey() {
        return "report.detail.title";
    }
}

