package fr.lampalon.lifemod.platform.bukkit.gui;

import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class LogLookupGui extends AbstractGui {

    private final List<LogEntry> entries;
    private final long totalCount;
    private final String targetName;
    private final UUID targetUuid;
    private final LogType filterType;
    private final int page;
    private final long fromTime;
    private final TimeRange timeRange;
    private final ReopenCallback reopenCallback;

    private static final Map<UUID, Consumer<String>> PENDING_INPUTS = new ConcurrentHashMap<>();
    private static boolean listenerRegistered = false;

    private static final class ChatInputListener implements Listener {
        @EventHandler
        public void onChat(AsyncPlayerChatEvent event) {
            Consumer<String> callback = PENDING_INPUTS.remove(event.getPlayer().getUniqueId());
            if (callback == null) return;
            event.setCancelled(true);
            Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LifeMod");
            if (plugin != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> callback.accept(event.getMessage()));
            }
        }
    }

    private static void ensureListenerRegistered() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LifeMod");
        if (plugin != null) {
            org.bukkit.Bukkit.getPluginManager().registerEvents(new ChatInputListener(), plugin);
        }
    }

    @FunctionalInterface
    public interface ReopenCallback {
        void reopen(LogType filterType, TimeRange timeRange, int page);
    }

    public static final class TimeRange {
        public static final TimeRange HOUR_1 = new TimeRange("HOUR_1", 1L * 60 * 60 * 1000, "§e1h");
        public static final TimeRange HOUR_6 = new TimeRange("HOUR_6", 6L * 60 * 60 * 1000, "§66h");
        public static final TimeRange DAY_1  = new TimeRange("DAY_1", 24L * 60 * 60 * 1000, "§b24h");
        public static final TimeRange DAY_7  = new TimeRange("DAY_7", 7L * 24 * 60 * 60 * 1000, "§a7d");
        public static final TimeRange DAY_30 = new TimeRange("DAY_30", 30L * 24 * 60 * 60 * 1000, "§c30d");
        public static final TimeRange ALL    = new TimeRange("ALL", Long.MAX_VALUE, "§5All");

        public static final List<TimeRange> PREDEFINED = Arrays.asList(HOUR_1, HOUR_6, DAY_1, DAY_7, DAY_30, ALL);

        final String enumName;
        public final long millis;
        public final String label;
        public final boolean isCustom;

        private TimeRange(String enumName, long millis, String label) {
            this.enumName = enumName;
            this.millis = millis;
            this.label = label;
            this.isCustom = false;
        }

        public TimeRange(long millis, String label) {
            this.enumName = null;
            this.millis = millis;
            this.label = label;
            this.isCustom = true;
        }

        public static TimeRange byName(String name) {
            for (TimeRange tr : PREDEFINED) {
                if (tr.enumName.equals(name)) return tr;
            }
            return DAY_7;
        }
    }

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LogLookupGui(Player player, List<LogEntry> entries, long totalCount,
                        String targetName, UUID targetUuid, LogType filterType, int page,
                        long fromTime, TimeRange timeRange, ReopenCallback reopenCallback) {
        super(player);
        this.entries = entries;
        this.totalCount = totalCount;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        this.filterType = filterType;
        this.page = page;
        this.fromTime = fromTime;
        this.timeRange = timeRange;
        this.reopenCallback = reopenCallback;
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
                        "@ # # < > F T # #")
                .addIngredient('.', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('#', createBorder())
                .addIngredient('@', createInfoItem())
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
                .addIngredient('F', createTypeFilterItem())
                .addIngredient('T', createTimeFilterItem())
                .setContent(buildItems())
                .build();
    }

    @Override
    protected String getTitleKey() {
        return "logs.gui.title";
    }

    @Override
    protected String getTitle() {
        return lang.getMessage(getTitleKey(), "%target%", targetName != null ? targetName : "?");
    }

    private void addLoreFromConfig(ItemBuilder builder, String path, String... placeholders) {
        List<String> lines = config.getStringList(path);
        if (lines == null || lines.isEmpty()) return;
        for (String line : lines) {
            if (placeholders.length > 0) {
                for (int i = 0; i < placeholders.length; i += 2) {
                    line = line.replace(placeholders[i], placeholders[i + 1]);
                }
            }
            builder.addLoreLines(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    private Item createInfoItem() {
        String typeLabel = filterType != null ? filterType.name() : "ALL";
        String timeLabel = timeRange != null ? timeRange.label : "7d";
        ItemBuilder builder = new ItemBuilder(Material.valueOf(
                config.getString("gui.logs.info-material", "BOOK")));
        builder.setDisplayName("§6" + targetName);
        addLoreFromConfig(builder, "gui.logs.info-lore",
                "%type%", typeLabel,
                "%range%", timeLabel,
                "%count%", String.valueOf(totalCount));
        return new SimpleItem(builder);
    }

    private Item createTypeFilterItem() {
        ItemBuilder builder = new ItemBuilder(Material.valueOf(
                config.getString("gui.logs.filter-type-material", "HOPPER")));
        builder.setDisplayName(lang.getMessage("logs.gui.filter-type-name"));
        addLoreFromConfig(builder, "gui.logs.filter-type-lore",
                "%type%", filterType != null ? filterType.name() : "ALL");
        return new SimpleItem(builder, click -> {
            if (click.getEvent().isShiftClick()) {
                if (reopenCallback != null) reopenCallback.reopen(null, timeRange, 1);
            } else {
                LogType next = nextType(filterType);
                if (reopenCallback != null) reopenCallback.reopen(next, timeRange, 1);
            }
        });
    }

    private Item createTimeFilterItem() {
        ItemBuilder builder = new ItemBuilder(Material.valueOf(
                config.getString("gui.logs.filter-time-material", "CLOCK")));
        builder.setDisplayName(lang.getMessage("logs.gui.filter-time-name"));
        for (TimeRange tr : TimeRange.PREDEFINED) {
            String prefix = tr == timeRange ? "§a▶ " : "  ";
            builder.addLoreLines(prefix + tr.label);
        }
        if (timeRange != null && timeRange.isCustom) {
            builder.addLoreLines("§a▶ " + timeRange.label);
        }
        addLoreFromConfig(builder, "gui.logs.filter-time-lore");
        return new SimpleItem(builder, click -> {
            if (click.getEvent().isShiftClick()) {
                if (reopenCallback != null) reopenCallback.reopen(filterType, TimeRange.DAY_7, 1);
            } else if (click.getEvent().isRightClick()) {
                player.closeInventory();
                player.sendMessage(lang.getMessage("logs.gui.custom-time-name"));
                ensureListenerRegistered();
                PENDING_INPUTS.put(player.getUniqueId(), new Consumer<String>() {
                    @Override
                    public void accept(String input) {
                        String trimmed = input.trim();
                        if (trimmed.equalsIgnoreCase("cancel")) {
                            if (reopenCallback != null) reopenCallback.reopen(filterType, timeRange, 1);
                            return;
                        }
                        long millis = parseTimeString(trimmed);
                        if (millis > 0) {
                            player.sendMessage(lang.getMessage("logs.gui.custom-time-success", "%time%", trimmed));
                            if (reopenCallback != null)
                                reopenCallback.reopen(filterType, new TimeRange(millis, "§6" + trimmed), 1);
                        } else {
                            player.sendMessage(lang.getMessage("logs.gui.custom-time-invalid"));
                            PENDING_INPUTS.put(player.getUniqueId(), this);
                        }
                    }
                });
            } else {
                TimeRange next = nextTimeRange(timeRange);
                if (reopenCallback != null) reopenCallback.reopen(filterType, next, 1);
            }
        });
    }

    private static long parseTimeString(String input) {
        if (input == null || input.isEmpty()) return -1;
        input = input.trim().toLowerCase();
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') {
                num.append(c);
            } else {
                if (num.length() == 0) continue;
                long val = Long.parseLong(num.toString());
                num.setLength(0);
                switch (c) {
                    case 's': total += val * 1000; break;
                    case 'm': total += val * 60000; break;
                    case 'h': total += val * 3600000; break;
                    case 'd': total += val * 86400000; break;
                    default: return -1;
                }
            }
        }
        return total > 0 ? total : -1;
    }

    private static LogType nextType(LogType current) {
        LogType[] values = LogType.values();
        if (current == null) return values[0];
        int idx = current.ordinal() + 1;
        return idx < values.length ? values[idx] : null;
    }

    private static TimeRange nextTimeRange(TimeRange current) {
        int idx = TimeRange.PREDEFINED.indexOf(current);
        if (idx == -1) return TimeRange.PREDEFINED.get(0); // custom → first predefined
        return TimeRange.PREDEFINED.get((idx + 1) % TimeRange.PREDEFINED.size());
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
            String readable = formatActionData(type, entry.getActionData());
            builder.addLoreLines("§7" + readable);
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

    private static String formatActionData(LogType type, String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) return "";
        String raw = json.replace("{", "").replace("}", "").replace("\"", "");
        String[] pairs = raw.split(",");
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        for (String pair : pairs) {
            int eq = pair.indexOf(':');
            if (eq > 0) {
                String k = pair.substring(0, eq).trim();
                String v = pair.substring(eq + 1).trim();
                map.put(k, v);
            }
        }
        if (type == null) return "Data: " + raw;
        switch (type) {
            case LOGIN_SUCCESS:
                return "§7IP: §f" + map.getOrDefault("ip", "?");
            case CHAT_MESSAGE:
            case PRIVATE_MESSAGE:
            case STAFF_CHAT_MESSAGE:
                return "§7Message: §f" + map.getOrDefault("m", "?");
            case COMMAND_EXECUTED:
            case COMMAND_BLOCKED:
                return "§7Command: §f" + map.getOrDefault("c", "?");
            case BLOCK_BREAK:
            case BLOCK_PLACE:
                return "§7Block: §f" + map.getOrDefault("b", "?");
            case BLOCK_BURN:
            case BLOCK_FADE:
                return "§7Block: §f" + map.getOrDefault("b", "?");
            case BLOCK_FORM:
                return "§7From: §f" + map.getOrDefault("s", "?") + " §7→ §f" + map.getOrDefault("f", "?");
            case BUCKET_FILL:
            case BUCKET_EMPTY:
                return "§7Bucket: §f" + map.getOrDefault("b", "?");
            case SIGN_CHANGE:
                return "§7Text: §f" + map.getOrDefault("t", "?");
            case EXPLOSION:
                return "§7Entity: §f" + map.getOrDefault("e", "?") + " §7×" + map.getOrDefault("s", "?");
            case CONTAINER_OPEN:
                return "§7Container: §f" + map.getOrDefault("c", "?");
            case CONTAINER_TAKE:
            case CONTAINER_PUT:
                return "§7Item: §f" + map.getOrDefault("i", "?") + " §7×" + map.getOrDefault("a", "1") + " §7(" + map.getOrDefault("c", "?") + ")";
            case ITEM_DROP:
            case ITEM_PICKUP:
                return "§7Item: §f" + map.getOrDefault("i", "?") + " §7×" + map.getOrDefault("a", "1");
            case ITEM_CONSUME:
                return "§7Item: §f" + map.getOrDefault("i", "?");
            case CRAFT:
                return "§7Crafted: §f" + map.getOrDefault("i", "?") + " §7×" + map.getOrDefault("a", "1");
            case ENCHANT:
                return "§7Item: §f" + map.getOrDefault("i", "?") + " §7(" + map.getOrDefault("e", "?") + ")";
            case FISHING:
                return "§7State: §f" + map.getOrDefault("s", "?");
            case VEHICLE_ENTER:
            case VEHICLE_EXIT:
                return "§7Vehicle: §f" + map.getOrDefault("v", "?");
            case ENTITY_TAME:
            case SHEEP_SHEAR:
                return "§7Entity: §f" + map.getOrDefault("e", "?");
            case DEATH_PLAYER:
                return "§7Killed by: §f" + map.getOrDefault("k", "?") + " §7with §f" + map.getOrDefault("w", "?");
            case KILL_PLAYER:
                return "§7Killed §f" + (map.containsKey("t") ? map.get("t") : "?") + " §7with §f" + map.getOrDefault("w", "?");
            case DEATH_MOB:
                return "§7Killed by: §f" + map.getOrDefault("m", "?");
            case DEATH_ENVIRONMENT:
                return "§7Cause: §f" + map.getOrDefault("c", "?");
            case KILL_MOB:
                return "§7Mob: §f" + map.getOrDefault("m", "?");
            case TELEPORT:
                return "§7Cause: §f" + map.getOrDefault("c", "?");
            case GAMEMODE_CHANGE:
                return "§7From: §f" + map.getOrDefault("f", "?") + " §7→ §f" + map.getOrDefault("t", "?");
            case ECONOMY_DEPOSIT:
            case ECONOMY_WITHDRAW:
                return "§7Amount: §f" + map.getOrDefault("a", "?") + " §7(Balance: §f" + map.getOrDefault("b", "?") + ")";
            case ECONOMY_TRANSFER:
                return "§7Amount: §f" + map.getOrDefault("a", "?") + " §7→ §f" + map.getOrDefault("t", "?");
            case SHOP_BUY:
            case SHOP_SELL:
                return "§7Item: §f" + map.getOrDefault("i", "?") + " §7×" + map.getOrDefault("a", "1") + " §7(§f" + map.getOrDefault("p", "?") + "§7)";
            case TRADE_PLUGIN:
            case TRADE_VILLAGER:
                return "§7Trade: §f" + map.getOrDefault("i", "?") + " §7×" + map.getOrDefault("a", "1");
            default:
                return "§7Data: §f" + raw;
        }
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
