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
        lore.add(lang.getMessage("sanctions.gui.item.target", "%target%", sanction.getPlayerName()));
        lore.add(lang.getMessage("sanctions.gui.item.type", "%type%", sanction.getType().name()));
        lore.add(lang.getMessage("sanctions.gui.item.reason", "%reason%", sanction.getReason()));
        lore.add(lang.getMessage("sanctions.gui.item.issuer", "%issuer%", sanction.getIssuerName()));
        lore.add(lang.getMessage("sanctions.gui.item.server", "%server%", sanction.getServerName()));
        lore.add(lang.getMessage("sanctions.gui.item.date", "%date%", sdf.format(new Date(sanction.getCreatedAt()))));
        
        lore.add("");
        if (sanction.isActive() && !sanction.isExpired()) {
            lore.add(lang.getMessage("sanctions.gui.item.active"));
            if (!sanction.isPermanent()) {
                lore.add(lang.getMessage("sanctions.gui.item.expire-date", "%date%", sdf.format(new Date(sanction.getExpirationTime()))));
            } else {
                lore.add(lang.getMessage("sanctions.gui.item.permanent"));
            }
        } else {
            String status = !sanction.isActive() ? lang.getMessage("sanctions.gui.item.revoked") : lang.getMessage("sanctions.gui.item.expired");
            lore.add(status);
            if (sanction.getRemovedByName() != null) {
                lore.add(lang.getMessage("sanctions.gui.item.revoked-by", "%player%", sanction.getRemovedByName()));
                lore.add(lang.getMessage("sanctions.gui.item.revoke-reason", "%reason%", sanction.getRemoveReason()));
            } else if (sanction.isActive() && sanction.isExpired()) {
                lore.add(lang.getMessage("sanctions.gui.item.expired-date", "%date%", sdf.format(new Date(sanction.getExpirationTime()))));
            }
        }

        lore.add("");
        lore.add(lang.getMessage("sanctions.gui.item.id", "%id%", sanction.getUuid().toString().substring(0, 8)));
        lore.add(lang.getMessage("sanctions.gui.item.delete"));
        lore.add("&8&m--------------------------");

        String title = lang.getMessage("sanctions.gui.item.title", "%id%", sanction.getUuid().toString().substring(0, 8));
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
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("sanctions.gui.item.delete-success", "&aSanction supprimée !")));
            if (onUpdate != null) onUpdate.run();
        }
    }
}
