package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.service.ISanctionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HistoryGui extends PagedAbstractGui {

    private List<Sanction> history;
    private final String targetName;
    private final UUID targetUuid;

    public HistoryGui(Player player, List<Sanction> history, String targetName, UUID targetUuid) {
        super(player);
        this.history = history;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
    }

    @Override
    protected List<Item> getListItems() {
        return history.stream()
                .map(s -> new SanctionItem(s, this::refresh))
                .collect(Collectors.toList());
    }

    private void refresh() {
        ServiceRegistry.get(ISanctionService.class).getHistory(targetUuid).thenAccept(newHistory -> {
            this.history = newHistory;
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("LifeMod"), this::open);
        });
    }

    @Override
    protected String getTitle() {
        return ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class).getMessage("history.gui.title", "%target%", targetName);
    }

    @Override
    protected String getTitleKey() {
        return ""; // Surchargé par getTitle()
    }
}

