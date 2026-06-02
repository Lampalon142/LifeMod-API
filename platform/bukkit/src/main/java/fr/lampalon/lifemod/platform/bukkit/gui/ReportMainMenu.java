package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Report;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;
import java.util.stream.Collectors;

public class ReportMainMenu extends PagedAbstractGui {

    private final List<Report> reports;

    public ReportMainMenu(Player player, List<Report> reports) {
        super(player);
        this.reports = reports;
    }

    @Override
    protected List<Item> getListItems() {
        return reports.stream()
                .map(ReportItem::new)
                .collect(Collectors.toList());
    }

    @Override
    protected String getTitleKey() {
        return "report.gui.title";
    }
}
