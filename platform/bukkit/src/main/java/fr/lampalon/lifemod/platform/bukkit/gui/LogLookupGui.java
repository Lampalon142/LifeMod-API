package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class LogLookupGui extends AbstractGui {

    private final List<LogEntry> entries;
    private final long totalCount;
    private final String targetName;
    private final UUID targetUuid;
    private final LogType filterType;
    private final int page;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LogLookupGui(Player player, List<LogEntry> entries, long totalCount,
                        String targetName, UUID targetUuid, LogType filterType, int page) {
        super(player);
        this.entries = entries;
        this.totalCount = totalCount;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        this.filterType = filterType;
        this.page = page;
    }

    @Override
    public Gui buildGui() {
        return PagedGui.items()
                .setStructure(
                        "# # # # # # # # #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# . . . . . . . #",
                        "# # # < # > # # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createBorder())
                .addIngredient('<', new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        ItemBuilder builder = new ItemBuilder(Material.valueOf(
                                config.getString("gui.pagination.previous-material", "ARROW")));
                        builder.setDisplayName(lang.getMessage("gui.pagination.previous"));
                        if (!gui.hasPreviousPage())
                            builder.addLoreLines(lang.getMessage("gui.pagination.first-page"));
                        return builder;
                    }
                })
                .addIngredient('>', new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        ItemBuilder builder = new ItemBuilder(Material.valueOf(
                                config.getString("gui.pagination.next-material", "ARROW")));
                        builder.setDisplayName(lang.getMessage("gui.pagination.next"));
                        if (!gui.hasNextPage())
                            builder.addLoreLines(lang.getMessage("gui.pagination.last-page"));
                        return builder;
                    }
                })
                .setContent(buildItems())
                .build();
    }

    @Override
    protected String getTitleKey() {
        return "logs.gui.title";
    }

    private List<Item> buildItems() {
        List<Item> items = new ArrayList<>();
        int totalPages = (int) Math.ceil((double) totalCount / 45);
        int pageSize = 45;

        for (LogEntry entry : entries) {
            items.add(createLogItem(entry));
        }

        while (items.size() < pageSize) {
            items.add(new SimpleItem(new ItemBuilder(Material.AIR)));
        }

        return items;
    }

    private Item createLogItem(LogEntry entry) {
        LogType type = getTypeSafe(entry.getType());
        Material icon = iconForType(type);
        String time = DATE_FMT.format(new Date(entry.getCreatedAt()));
        String typeName = type != null ? type.name() : "UNKNOWN";

        ItemBuilder builder = new ItemBuilder(icon);
        builder.setDisplayName("§e" + typeName);

        builder.addLoreLines("§7" + time);
        if (entry.getPlayerName() != null) builder.addLoreLines("§7Player: §f" + entry.getPlayerName());
        if (entry.getTargetName() != null) builder.addLoreLines("§7Target: §f" + entry.getTargetName());
        if (entry.getActionData() != null && !entry.getActionData().equals("{}") && !entry.getActionData().isEmpty()) {
            builder.addLoreLines("§7Data: §f" + entry.getActionData());
        }
        if (entry.getWorld() != null) {
            builder.addLoreLines("§7At: §f" + entry.getX() + " " + entry.getY() + " " + entry.getZ() + " (" + entry.getWorld() + ")");
        }
        if (entry.getServerName() != null) builder.addLoreLines("§7Server: §f" + entry.getServerName());

        return new SimpleItem(builder);
    }

    private static LogType getTypeSafe(int ordinal) {
        LogType[] values = LogType.values();
        if (ordinal >= 0 && ordinal < values.length) return values[ordinal];
        return null;
    }

    private static Material iconForType(LogType type) {
        if (type == null) return Material.MAP;
        switch (type) {
            case LOGIN_SUCCESS: case LOGIN_FAILED_BANNED:
            case LOGIN_FAILED_WHITELIST: case LOGIN_FAILED_VERSION:
            case LOGIN_FAILED_OTHER: case QUIT: case KICK:
            case TIMEOUT: case NAME_CHANGE:
                return Material.OAK_DOOR;
            case CHAT_MESSAGE: case PRIVATE_MESSAGE: case STAFF_CHAT_MESSAGE:
                return Material.PAPER;
            case COMMAND_EXECUTED: case COMMAND_BLOCKED:
                return Material.COMMAND_BLOCK;
            case BLOCK_BREAK: return Material.DIAMOND_PICKAXE;
            case BLOCK_PLACE: return Material.GRASS_BLOCK;
            case CONTAINER_OPEN: case CONTAINER_TAKE: case CONTAINER_PUT:
                return Material.CHEST;
            case BUCKET_FILL: case BUCKET_EMPTY:
                return Material.WATER_BUCKET;
            case ITEM_PICKUP: case ITEM_DROP: case ITEM_CONSUME:
            case CRAFT: case ENCHANT: case ANVIL_USE: case GRINDSTONE_USE:
            case FURNACE_SMELT: case BREW:
                return Material.DIAMOND;
            case TELEPORT: case PORTAL_USE: case WORLD_CHANGE:
                return Material.ENDER_PEARL;
            case DEATH_PLAYER: case DEATH_MOB: case DEATH_ENVIRONMENT:
            case DEATH_OTHER: case KILL_PLAYER: case KILL_MOB:
            case RESPAWN:
                return Material.SKELETON_SKULL;
            case GAMEMODE_CHANGE: return Material.COMPASS;
            case ECONOMY_DEPOSIT: case ECONOMY_WITHDRAW: case ECONOMY_TRANSFER:
                return Material.EMERALD;
            case TRADE_VILLAGER: case TRADE_PLUGIN: case SHOP_BUY: case SHOP_SELL:
                return Material.EMERALD_BLOCK;
            case SANCTION_APPLY: case SANCTION_REMOVE: case SANCTION_EXPIRE:
                return Material.BARRIER;
            case VANISH_ENABLE: case VANISH_DISABLE:
                return Material.GLASS;
            case FREEZE_PLAYER: case UNFREEZE_PLAYER:
                return Material.ICE;
            case STAFF_MODE_ENABLE: case STAFF_MODE_DISABLE:
                return Material.BLAZE_ROD;
            case SERVER_START: case SERVER_STOP: case RELOAD:
                return Material.REDSTONE_BLOCK;
            default:
                return Material.MAP;
        }
    }
}
