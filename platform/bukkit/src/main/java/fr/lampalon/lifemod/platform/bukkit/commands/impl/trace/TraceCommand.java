package fr.lampalon.lifemod.platform.bukkit.commands.impl.trace;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogEntry;
import fr.lampalon.lifemod.common.model.LogQuery;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.common.service.ILogService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TraceCommand extends LifeCommand {

    private static final NamespacedKey TRACE_KEY = new NamespacedKey(LifeMod.getInstance(), "trace_id");
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TraceCommand() {
        super("trace", "lifemod.trace", true);
        setDescription("Trace an item's history through all players.");
        setUsage("/trace");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            player.sendMessage(context.getLang().getMessage("trace.no-item"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(context.getLang().getMessage("trace.no-trace"));
            return;
        }

        String traceId = meta.getPersistentDataContainer().get(TRACE_KEY, PersistentDataType.STRING);
        if (traceId == null) {
            player.sendMessage(context.getLang().getMessage("trace.no-trace"));
            return;
        }

        ILogService logService = ServiceRegistry.get(ILogService.class);
        if (logService == null) {
            player.sendMessage(context.getLang().getMessage("logs.disabled"));
            return;
        }

        LogQuery query = LogQuery.builder()
                .type(LogType.ITEM_TRACE)
                .targetUuid(UUID.fromString(traceId))
                .fromTime(0)
                .limit(200)
                .offset(0)
                .build();

        logService.query(query).thenAccept(entries -> {
            Bukkit.getScheduler().runTask(LifeMod.getInstance(), () -> {
                if (entries.isEmpty()) {
                    player.sendMessage(context.getLang().getMessage("trace.no-results"));
                    return;
                }
                player.sendMessage(context.getLang().getMessage("trace.header",
                        "%item%", item.getType().name()));
                int n = 1;
                for (LogEntry entry : entries) {
                    String time = DATE_FMT.format(new Date(entry.getCreatedAt()));
                    String action = parseAction(entry.getActionData(), context);
                    String pName = entry.getPlayerName() != null ? entry.getPlayerName() : "?";
                    String loc = entry.getWorld() != null
                            ? entry.getWorld() + " (" + entry.getX() + ", " + entry.getY() + ", " + entry.getZ() + ")"
                            : "?";
                    player.sendMessage(context.getLang().getMessage("trace.entry",
                            "%num%", String.valueOf(n++),
                            "%player%", pName,
                            "%action%", action,
                            "%location%", loc,
                            "%time%", time));
                }
                player.sendMessage(context.getLang().getMessage("trace.footer"));
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            player.sendMessage(context.getLang().getMessage("trace.error"));
            return null;
        });
    }

    private static String parseAction(String actionData, CommandContext context) {
        if (actionData == null || actionData.isEmpty()) return "?";
        String raw = actionData.replace("{", "").replace("}", "").replace("\"", "");
        String action = "?";
        for (String pair : raw.split(",")) {
            int eq = pair.indexOf(':');
            if (eq > 0) {
                String k = pair.substring(0, eq).trim();
                String v = pair.substring(eq + 1).trim();
                if (k.equals("action")) {
                    action = v;
                    break;
                }
            }
        }
        return context.getLang().getMessage("trace.action." + action);
    }
}
