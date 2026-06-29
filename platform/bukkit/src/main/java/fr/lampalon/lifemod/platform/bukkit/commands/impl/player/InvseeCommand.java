package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.InventoryUtil;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvseeCommand extends LifeCommand {
    private final LifeMod plugin;

    public InvseeCommand(LifeMod plugin) {
        super("invsee", "lifemod.invsee", true);
        this.plugin = plugin;
        setDescription("Views the inventory of a player (online or offline).");
        setUsage("/invsee <player>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.invsee.usage"));
            return;
        }

        Player onlineTarget = Bukkit.getPlayer(args[0]);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            Inventory inv = createOnlineInventory(onlineTarget, context);
            plugin.getInvseeManager().startViewing(player, onlineTarget);
            player.openInventory(inv);
            player.sendMessage(context.getLang().getMessage("commands.invsee.opened",
                "%target%", onlineTarget.getName(),
                "%status%", "§a(Online)"));
            plugin.getLogger().info("[InvseeDebug] " + player.getName() + " opened online inventory of " + onlineTarget.getName());
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
            if (!offlineTarget.hasPlayedBefore()) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            DatabaseProvider db = plugin.getDatabaseManager().getDatabaseProvider();
            UUID targetUuid = offlineTarget.getUniqueId();

            plugin.getLogger().info("[InvseeDebug] Fetching raw inventory for " + args[0] + " uuid=" + targetUuid + " server=" + plugin.getServerName());
            byte[] rawData = db.getRawInventory(targetUuid, plugin.getServerName());
            plugin.getLogger().info("[InvseeDebug] rawData " + (rawData == null ? "null" : ("length=" + rawData.length + " bytes")));

            if (rawData == null) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.no-inventory", "%target%", args[0]));
                return;
            }

            Inventory inv;
            try {
                plugin.getLogger().info("[InvseeDebug] Attempting InventoryUtil.deserializeFullInventory...");
                InventoryUtil.FullInventory full = InventoryUtil.deserializeFullInventory(rawData);
                plugin.getLogger().info("[InvseeDebug] deserializeFullInventory OK: contents=" + full.contents().length + " armor=" + full.armor().length);
                inv = buildOfflineInventory(full.contents(), full.armor(), args[0], context);
            } catch (Exception e) {
                plugin.getLogger().info("[InvseeDebug] deserializeFullInventory failed: " + e.getClass().getName() + " msg='" + e.getMessage() + "'");
                e.printStackTrace();

                // Fallback: try old BukkitDatabaseUtil format (flat 36-slot, no armor)
                try {
                    plugin.getLogger().info("[InvseeDebug] Trying BukkitDatabaseUtil format fallback...");
                    ItemStack[] flat = BukkitDatabaseUtil.deserializeInventory(rawData);
                    if (flat == null) throw new RuntimeException("BukkitDatabaseUtil returned null");
                    plugin.getLogger().info("[InvseeDebug] Fallback OK: items=" + flat.length);
                    inv = buildOfflineInventory(flat, new ItemStack[0], args[0], context);
                } catch (Exception e2) {
                    plugin.getLogger().info("[InvseeDebug] Fallback also failed: " + e2.getClass().getName() + " msg='" + e2.getMessage() + "'");
                    e2.printStackTrace();
                    context.getSender().sendMessage("§cFailed to load inventory: " + e2.getClass().getSimpleName());
                    return;
                }
            }

            plugin.getInvseeManager().startOfflineViewing(player, offlineTarget.getUniqueId());
            player.openInventory(inv);
            player.sendMessage(context.getLang().getMessage("commands.invsee.opened",
                "%target%", args[0],
                "%status%", "§c(Offline)"));
            plugin.getLogger().info("[InvseeDebug] " + player.getName() + " opened offline inventory of " + args[0]);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "generic", Map.of("%target%", args[0]));
        }
    }

    private Inventory createOnlineInventory(Player target, CommandContext context) {
        String invTitle = context.getLang().getMessage("commands.invsee.name", "%player%", target.getName());
        Inventory targetInventory = Bukkit.createInventory(null, 45, invTitle);
        PlayerInventory targetPlayerInventory = target.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack item = targetPlayerInventory.getItem(i);
            if (item != null) {
                targetInventory.setItem(i, item);
            }
        }
        targetInventory.setItem(36, targetPlayerInventory.getHelmet());
        targetInventory.setItem(37, targetPlayerInventory.getChestplate());
        targetInventory.setItem(38, targetPlayerInventory.getLeggings());
        targetInventory.setItem(39, targetPlayerInventory.getBoots());

        return targetInventory;
    }

    private Inventory buildOfflineInventory(ItemStack[] contents, ItemStack[] armor, String targetName, CommandContext context) {
        String invTitle = context.getLang().getMessage("commands.oinvsee.name", "%target%", targetName);
        Inventory inv = Bukkit.createInventory(null, 45, invTitle);

        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] != null) inv.setItem(i, contents[i]);
        }
        if (armor.length > 3) inv.setItem(36, armor[3]); // helmet
        if (armor.length > 2) inv.setItem(37, armor[2]); // chestplate
        if (armor.length > 1) inv.setItem(38, armor[1]); // leggings
        if (armor.length > 0) inv.setItem(39, armor[0]); // boots

        return inv;
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
