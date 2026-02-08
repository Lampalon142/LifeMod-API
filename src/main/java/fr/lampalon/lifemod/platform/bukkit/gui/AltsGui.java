package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
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
import java.util.stream.Collectors;

public class AltsGui extends PagedAbstractGui {

    private final List<PlayerData> alts;
    private final String ip;

    public AltsGui(Player player, List<PlayerData> alts, String ip) {
        super(player);
        this.alts = alts;
        this.ip = ip;
    }

    @Override
    protected List<Item> getListItems() {
        return alts.stream()
                .map(AltItem::new)
                .collect(Collectors.toList());
    }

    @Override
    protected String getTitle() {
        return ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class).getMessage("alts.gui.title", "%ip%", ip);
    }

    @Override
    protected String getTitleKey() {
        return "";
    }

    private class AltItem extends AbstractItem {
        private final PlayerData data;

        public AltItem(PlayerData data) {
            this.data = data;
        }

        @Override
        public ItemProvider getItemProvider() {
            String status;
            Material material;
            
            ISanctionService ss = ServiceRegistry.get(ISanctionService.class);
            fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            Sanction ban = ss.getActiveSanction(data.getUuid(), data.getLastName(), SanctionType.BAN).join();
            
            if (ban != null && !ban.isExpired()) {
                status = lang.getMessage("alts.gui.status.banned");
                material = Material.RED_TERRACOTTA;
            } else if (Bukkit.getPlayer(data.getUuid()) != null) {
                status = lang.getMessage("alts.gui.status.online");
                material = Material.LIME_TERRACOTTA;
            } else {
                status = lang.getMessage("alts.gui.status.offline");
                material = Material.GRAY_TERRACOTTA;
            }

            return new ItemBuilder(material)
                    .setDisplayName(MessageUtil.formatMessage(lang.getMessage("alts.gui.item.name", "%player%", data.getLastName())))
                    .addLoreLines(
                            MessageUtil.formatMessage(lang.getMessage("alts.gui.item.status", "%status%", status)),
                            MessageUtil.formatMessage(lang.getMessage("alts.gui.item.uuid", "%uuid%", data.getUuid().toString())),
                            "",
                            MessageUtil.formatMessage(lang.getMessage("alts.gui.item.history")),
                            MessageUtil.formatMessage(lang.getMessage("alts.gui.item.case"))
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
