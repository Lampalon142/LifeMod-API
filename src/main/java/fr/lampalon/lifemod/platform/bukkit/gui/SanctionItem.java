package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Sanction;
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

    public SanctionItem(Sanction sanction) {
        this.sanction = sanction;
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

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        List<String> lore = new ArrayList<>();
        lore.add("&7Type: &f" + sanction.getType().name());
        lore.add("&7Raison: &f" + sanction.getReason());
        lore.add("&7Par: &e" + sanction.getIssuerName());
        lore.add("&7Date: &f" + sdf.format(new Date(sanction.getCreatedAt())));
        
        if (sanction.isActive()) {
            lore.add("&7Status: &aACTIF");
            if (!sanction.isPermanent()) {
                lore.add("&7Expire le: &e" + sdf.format(new Date(sanction.getExpirationTime())));
            } else {
                lore.add("&7Expire le: &cPermanent");
            }
        } else {
            lore.add("&7Status: &cREVOQUÉ/EXPIRÉ");
            if (sanction.getRemovedByName() != null) {
                lore.add("&7Levé par: &e" + sanction.getRemovedByName());
                lore.add("&7Raison: &f" + sanction.getRemoveReason());
            }
        }

        ItemBuilder builder = new ItemBuilder(material).setDisplayName("&6Sanction &8#" + sanction.getUuid().toString().substring(0, 8));
        for (String line : lore) {
            builder.addLoreLines(MessageUtil.formatMessage(line));
        }
        
        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
    }
}
