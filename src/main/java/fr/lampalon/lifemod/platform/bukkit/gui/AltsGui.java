package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AltsGui extends PagedAbstractGui {

    private final List<PlayerData> alts;
    private final String ip;
    private final Map<UUID, Sanction> bans;

    public AltsGui(Player player, List<PlayerData> alts, String ip, Map<UUID, Sanction> bans) {
        super(player);
        this.alts = alts;
        this.ip = ip;
        this.bans = bans;
    }

    @Override
    protected List<Item> getListItems() {
        return alts.stream()
                .map(data -> new AltItem(data, bans.get(data.getUuid())))
                .collect(Collectors.toList());
    }

    @Override
    protected String getTitle() {
        return ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class).getMessage("gui.alts.title", "%ip%", ip);
    }

    @Override
    protected String getTitleKey() {
        return "";
    }

    private class AltItem extends AbstractItem {
        private final PlayerData data;
        private final Sanction ban;

        public AltItem(PlayerData data, Sanction ban) {
            this.data = data;
            this.ban = ban;
        }

        @Override
        public ItemProvider getItemProvider() {
            String status;
            Material material;

            if (ban != null && !ban.isExpired()) {
                status = lang.getMessage("gui.alts.status-banned");
                material = Material.valueOf(config.getString("gui.alts.materials.banned", "RED_TERRACOTTA"));
            } else if (Bukkit.getPlayer(data.getUuid()) != null) {
                status = lang.getMessage("gui.alts.status-online");
                material = Material.valueOf(config.getString("gui.alts.materials.online", "LIME_TERRACOTTA"));
            } else {
                status = lang.getMessage("gui.alts.status-offline");
                material = Material.valueOf(config.getString("gui.alts.materials.offline", "GRAY_TERRACOTTA"));
            }

            return new ItemBuilder(material)
                    .setDisplayName(lang.getMessage("gui.alts.item-name", "%player%", data.getLastName()))
                    .addLoreLines(
                            lang.getMessage("gui.alts.item-status", "%status%", status),
                            lang.getMessage("gui.alts.item-uuid", "%uuid%", data.getUuid().toString()),
                            "",
                            lang.getMessage("gui.alts.item-history"),
                            lang.getMessage("gui.alts.item-case")
                    );
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if (clickType.isLeftClick()) {
                player.performCommand("history " + data.getLastName());
            } else if (clickType.isRightClick()) {
                player.performCommand("case " + data.getLastName());
            }
        }
    }
}
