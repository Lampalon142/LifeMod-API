package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.Report;
import fr.lampalon.lifemod.common.model.StaffNote;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StaffNoteItem extends AbstractItem {

    private final Report report;
    private final StaffNote note;

    public StaffNoteItem(Report report, StaffNote note) {
        this.report = report;
        this.note = note;
    }

    @Override
    public ItemProvider getItemProvider() {
        String authorName = Bukkit.getOfflinePlayer(note.getAuthor()).getName();
        if (authorName == null) authorName = note.getAuthor().toString();

        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        fr.lampalon.lifemod.common.service.IConfigurationService config = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.IConfigurationService.class);
        String dateFormat = config.getString("server.date-format", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

        ItemBuilder builder = new ItemBuilder(Material.PAPER)
                .setDisplayName(MessageUtil.formatMessage(lang.getMessage("reports.gui.notes.item-title").replace("%author%", authorName)))
                .addLoreLines(
                        MessageUtil.formatMessage(lang.getMessage("reports.gui.notes.item-date", "§7Date: §f%date%").replace("%date%", sdf.format(new Date(note.getCreatedAt())))),
                        "",
                        "§f" + note.getContent(),
                        "",
                        MessageUtil.formatMessage(lang.getMessage("reports.gui.notes.item-edit", "§eLeft-click to edit")),
                        MessageUtil.formatMessage(lang.getMessage("reports.gui.notes.item-delete", "§cRight-click to delete"))
                );

        return builder;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        if (clickType.isLeftClick()) {
            player.closeInventory();
            LifeMod.getInstance().getNoteInputManager().startNoteEdit(player, report, note);
            player.sendMessage(MessageUtil.formatMessage(lang.getMessage("reports.gui.notes.prompt")));
        } else if (clickType.isRightClick()) {
            report.removeStaffNote(note);
            LifeMod.getInstance().getDatabaseManager().getDatabaseProvider().deleteStaffNote(note.getNoteId());
            player.sendMessage(MessageUtil.formatMessage(lang.getMessage("reports.gui.notes.delete-success")));
            new StaffNotesGui(player, report).open();
        }
    }
}
