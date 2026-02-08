package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SanctionItem extends AbstractItem {

    private final Sanction sanction;
    private final Runnable onUpdate;

    public SanctionItem(Sanction sanction, Runnable onUpdate) {
        this.sanction = sanction;
        this.onUpdate = onUpdate;
    }

    @Override
    public ItemProvider getItemProvider() {
        Material material;
        switch (sanction.getType()) {
            case BAN: material = Material.RED_CONCRETE; break;
            case MUTE: material = Material.ORANGE_CONCRETE; break;
            case WARN: material = Material.YELLOW_CONCRETE; break;
            case KICK: material = Material.GRAY_CONCRETE; break;
            default: material = Material.PAPER;
        }

        String dateFormat = LifeMod.getInstance().getConfigConfig().getString("date-format", "dd/MM/yyyy HH:mm");
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        
        List<String> lore = new ArrayList<>();
        lore.add("&8&m--------------------------");
        lore.add(lang.getMessage("gui.history.item.target", "%target%", sanction.getPlayerName()));
        lore.add(lang.getMessage("gui.history.item.type", "%type%", sanction.getType().name()));
        lore.add(lang.getMessage("gui.history.item.reason", "%reason%", sanction.getReason()));
        lore.add(lang.getMessage("gui.history.item.issuer", "%issuer%", sanction.getIssuerName()));
        lore.add(lang.getMessage("gui.history.item.server", "%server%", sanction.getServerName()));
        lore.add(lang.getMessage("gui.history.item.date", "%date%", sdf.format(new Date(sanction.getCreatedAt()))));
        
        lore.add("");
        if (sanction.isActive() && !sanction.isExpired()) {
            lore.add(lang.getMessage("gui.history.item.status-active"));
            if (!sanction.isPermanent()) {
                lore.add(lang.getMessage("gui.history.item.expire-date", "%date%", sdf.format(new Date(sanction.getExpirationTime()))));
            } else {
                lore.add(lang.getMessage("gui.history.item.permanent"));
            }
        } else {
            String status = !sanction.isActive() ? lang.getMessage("gui.history.item.status-revoked") : lang.getMessage("gui.history.item.status-expired");
            lore.add(status);
            if (sanction.getRemovedByName() != null) {
                lore.add(lang.getMessage("gui.history.item.revoked-by", "%player%", sanction.getRemovedByName()));
                lore.add(lang.getMessage("gui.history.item.revoke-reason", "%reason%", sanction.getRemoveReason()));
            } else if (sanction.isActive() && sanction.isExpired()) {
                lore.add(lang.getMessage("gui.history.item.expired-date", "%date%", sdf.format(new Date(sanction.getExpirationTime()))));
            }
        }

        lore.add("");
        lore.add(lang.getMessage("gui.history.item.delete-info", "%id%", sanction.getUuid().toString().substring(0, 8)));
        lore.add("&8&m--------------------------");

        String title = lang.getMessage("gui.history.item.title", "%id%", sanction.getUuid().toString().substring(0, 8));
        ItemBuilder builder = new ItemBuilder(material).setDisplayName(MessageUtil.formatMessage(title));
        for (String line : lore) {
            builder.addLoreLines(MessageUtil.formatMessage(line));
        }
        
        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType.isRightClick()) {
            LifeMod.getInstance().getDatabaseManager().getDatabaseProvider().deleteSanction(sanction.getUuid());
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("gui.history.item.delete-success", "&aSanction deleted!")));
            if (onUpdate != null) onUpdate.run();
        }
    }
}
