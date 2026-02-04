package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Sanction;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;
import java.util.stream.Collectors;

public class HistoryGui extends PagedAbstractGui {

    private final List<Sanction> history;
    private final String targetName;

    public HistoryGui(Player player, List<Sanction> history, String targetName) {
        super(player);
        this.history = history;
        this.targetName = targetName;
    }

    @Override
    protected List<Item> getListItems() {
        return history.stream()
                .map(SanctionItem::new)
                .collect(Collectors.toList());
    }

    @Override
    protected String getTitle() {
        return "§6Historique: §e" + targetName;
    }

    @Override
    protected String getTitleKey() {
        return ""; // Surchargé par getTitle()
    }
}

