package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StaffHistoryGui extends PagedAbstractGui {

    private final List<Sanction> allSanctions;
    private List<Sanction> displayedSanctions;
    private final String staffName;
    private final UUID staffUuid;

    // Filters
    private SanctionType filterType = null;
    private String filterPlayer = null;
    private long filterTime = 0; // 0 = all time, otherwise cutoff timestamp

    public StaffHistoryGui(Player player, List<Sanction> sanctions, String staffName, UUID staffUuid) {
        super(player);
        this.allSanctions = sanctions;
        this.staffName = staffName;
        this.staffUuid = staffUuid;
        updateFilteredList();
    }

    private void updateFilteredList() {
        this.displayedSanctions = allSanctions.stream()
                .filter(s -> filterType == null || s.getType() == filterType)
                .filter(s -> filterPlayer == null || s.getPlayerName().toLowerCase().contains(filterPlayer.toLowerCase()))
                .filter(s -> filterTime == 0 || s.getCreatedAt() >= filterTime)
                .collect(Collectors.toList());
    }

    @Override
    protected List<Item> getListItems() {
        return displayedSanctions.stream()
                .map(s -> new SanctionItem(s, this::refresh))
                .collect(Collectors.toList());
    }

    private void refresh() {
        ServiceRegistry.get(ISanctionService.class).getSanctionsIssuedBy(staffName, staffUuid).thenAccept(newHistory -> {
            allSanctions.clear();
            allSanctions.addAll(newHistory);
            updateFilteredList();
            Bukkit.getScheduler().runTask(LifeMod.getInstance(), () -> {
                // To refresh properly with InvUI, usually we rebuild or update content. 
                // Since this is a PagedGui, rebuilding is often easier if content changed drastically.
                // However, PagedAbstractGui doesn't expose the GUI object easily for content update.
                // We'll just close and reopen for simplicity or use the 'refresh' method if we had one.
                this.open();
            });
        });
    }

    @Override
    public Gui buildGui() {
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        return PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# F P T < # > # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createBorder())
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        Material mat = Material.valueOf(config.getString("gui.pagination.previous-material", "ARROW"));
                        return new ItemBuilder(mat).setDisplayName(lang.getMessage("gui.pagination.previous"));
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        Material mat = Material.valueOf(config.getString("gui.pagination.next-material", "ARROW"));
                        return new ItemBuilder(mat).setDisplayName(lang.getMessage("gui.pagination.next"));
                    }
                })
                .addIngredient('F', new AbstractItem() { // Filter Type
                    @Override
                    public ItemProvider getItemProvider() {
                        String current = filterType == null ? lang.getMessage("gui.staffhistory.type-all") : filterType.name();
                        Material mat = Material.valueOf(config.getString("gui.staffhistory.filter-type-material", "HOPPER"));
                        return new ItemBuilder(mat)
                                .setDisplayName(lang.getMessage("gui.staffhistory.filter-type"))
                                .addLoreLines(lang.getMessage("gui.staffhistory.current", "%current%", current), lang.getMessage("gui.staffhistory.click-to-change"));
                    }
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                        if (filterType == null) filterType = SanctionType.BAN;
                        else if (filterType == SanctionType.BAN) filterType = SanctionType.MUTE;
                        else if (filterType == SanctionType.MUTE) filterType = SanctionType.WARN;
                        else if (filterType == SanctionType.WARN) filterType = SanctionType.KICK;
                        else filterType = null;
                        updateFilteredList();
                        open();
                    }
                })
                .addIngredient('P', new AbstractItem() { // Filter Player
                    @Override
                    public ItemProvider getItemProvider() {
                        String current = filterPlayer == null ? lang.getMessage("gui.staffhistory.type-all") : filterPlayer;
                        Material mat = Material.valueOf(config.getString("gui.staffhistory.filter-player-material", "PLAYER_HEAD"));
                        return new ItemBuilder(mat)
                                .setDisplayName(lang.getMessage("gui.staffhistory.filter-player"))
                                .addLoreLines(lang.getMessage("gui.staffhistory.current", "%current%", current), lang.getMessage("gui.staffhistory.click-to-chat"));
                    }
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                        player.closeInventory();
                        player.sendMessage(lang.getMessage("gui.staffhistory.chat-prompt"));
                        LifeMod.getInstance().getChatManager().awaitChatInput(player, input -> {
                            if (input.equalsIgnoreCase("none") || input.equalsIgnoreCase("cancel")) {
                                filterPlayer = null;
                            } else {
                                filterPlayer = input;
                            }
                            updateFilteredList();
                            Bukkit.getScheduler().runTask(LifeMod.getInstance(), () -> open());
                        });
                    }
                })
                .addIngredient('T', new AbstractItem() { // Filter Time
                    @Override
                    public ItemProvider getItemProvider() {
                        String current = filterTime == 0 ? lang.getMessage("gui.staffhistory.time-all") : lang.getMessage("gui.staffhistory.time-since");
                        Material mat = Material.valueOf(config.getString("gui.staffhistory.filter-date-material", "CLOCK"));
                        return new ItemBuilder(mat)
                                .setDisplayName(lang.getMessage("gui.staffhistory.filter-date"))
                                .addLoreLines(lang.getMessage("gui.staffhistory.current", "%current%", current), lang.getMessage("gui.staffhistory.click-to-change"));
                    }
                    @Override
                    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                        long now = System.currentTimeMillis();
                        if (filterTime == 0) filterTime = now - 3600000L;
                        else if (filterTime == now - 3600000L) filterTime = now - 86400000L;
                        else if (filterTime == now - 86400000L) filterTime = now - 604800000L;
                        else filterTime = 0;
                        updateFilteredList();
                        open();
                    }
                })
                .setContent(getListItems())
                .build();
    }

    @Override
    protected String getTitle() {
        return ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class).getMessage("staffhistory.title", "%staff%", staffName);
    }

    @Override
    protected String getTitleKey() {
        return "";
    }
}
