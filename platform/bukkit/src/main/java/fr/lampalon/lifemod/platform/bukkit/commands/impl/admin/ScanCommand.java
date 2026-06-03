package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.adapter.IItemsAdderService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.model.ScanResult;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScanCommand extends LifeCommand {
    private final LifeMod plugin;
    private final IItemsAdderService itemsAdderService;

    public ScanCommand(LifeMod plugin) {
        super("scan", "lifemod.admin.scan", true);
        this.plugin = plugin;
        this.itemsAdderService = ServiceRegistry.get(IItemsAdderService.class);
        setDescription("Scans the map and inventories for specific items.");
        setUsage("/scan <map|inventories|enderchest|all> <player|all> <item|hand>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 3) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.scan.usage"));
            return;
        }

        String type = context.getArgs()[0];
        String targetStr = context.getArgs()[1];
        String itemStr = context.getArgs()[2];

        ItemStack targetItem = null;
        if (itemStr.equalsIgnoreCase("hand")) {
            targetItem = context.getPlayer().getInventory().getItemInMainHand();
            if (targetItem == null || targetItem.getType() == Material.AIR) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.scan.hand-empty"));
                return;
            }
        } else {
            if (itemsAdderService != null && itemsAdderService.isEnabled()) {
                targetItem = itemsAdderService.getItem(itemStr);
            }

            if (targetItem == null) {
                Material material = Material.matchMaterial(itemStr);
                if (material != null) {
                    targetItem = new ItemStack(material);
                }
            }
        }

        if (targetItem == null) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.scan.invalid-item", "%item%", itemStr));
            return;
        }

        String itemName = targetItem.getType().name();
        if (itemsAdderService != null && itemsAdderService.isItemsAdderItem(targetItem)) {
            itemName = itemsAdderService.getItemId(targetItem);
        }

        context.getSender().sendMessage(context.getLang().getMessage("commands.scan.starting", "%item%", itemName));

        plugin.getScanManager().scan(type, targetStr, targetItem, progress -> {
            context.getSender().sendMessage(context.getLang().getMessage("commands.scan.progress", "%message%", progress));
        }).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> sendReport(context.getSender(), result, context));
            IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
            if (ph != null) {
                Map<String, Object> props = new HashMap<>();
                props.put("results_count", result.getTotalCount());
                ph.capture("lifemod_scan", props);
            }
        });
    }

    private void sendReport(org.bukkit.command.CommandSender sender, ScanResult result, CommandContext context) {
        sender.sendMessage(context.getLang().getMessage("commands.scan.report-header"));
        sender.sendMessage(context.getLang().getMessage("commands.scan.report-title"));
        sender.sendMessage(" ");
        sender.sendMessage(context.getLang().getMessage("commands.scan.report-total", "%count%", String.valueOf(result.getTotalCount())));
        sender.sendMessage(context.getLang().getMessage("commands.scan.report-duration", "%time%", String.valueOf(result.getDuration())));

        if (!result.getFoundPlayers().isEmpty()) {
            sender.sendMessage(" ");
            sender.sendMessage(context.getLang().getMessage("commands.scan.report-players-header"));
            result.getFoundPlayers().forEach((uuid, count) -> {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                sender.sendMessage(context.getLang().getMessage("commands.scan.report-player-entry", "%name%", name != null ? name : uuid.toString(), "%count%", String.valueOf(count)));
            });
        }

        if (!result.getFoundLocations().isEmpty()) {
            sender.sendMessage(" ");
            sender.sendMessage(context.getLang().getMessage("commands.scan.report-containers-header"));
            int limit = 10;
            int count = 0;
            for (var entry : result.getFoundLocations().entrySet()) {
                if (count >= limit) {
                    sender.sendMessage(context.getLang().getMessage("commands.scan.report-more", "%count%", String.valueOf(result.getFoundLocations().size() - limit)));
                    break;
                }
                Location loc = entry.getKey();
                sender.sendMessage(context.getLang().getMessage("commands.scan.report-container-entry",
                        "%world%", loc.getWorld().getName(),
                        "%x%", String.valueOf(loc.getBlockX()),
                        "%y%", String.valueOf(loc.getBlockY()),
                        "%z%", String.valueOf(loc.getBlockZ()),
                        "%count%", String.valueOf(entry.getValue())));
                count++;
            }
        }

        sender.sendMessage(context.getLang().getMessage("commands.scan.report-header"));
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return Arrays.asList("map", "inventories", "enderchest", "all").stream()
                    .filter(s -> s.startsWith(context.getArgs()[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (context.getArgs().length == 2) {
            List<String> targets = new ArrayList<>();
            targets.add("all");
            targets.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return targets.stream()
                    .filter(s -> s.startsWith(context.getArgs()[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (context.getArgs().length == 3) {
            List<String> items = new ArrayList<>();
            items.add("hand");
            // Limit to some common materials or let it be empty for performance
            return items.stream()
                    .filter(s -> s.startsWith(context.getArgs()[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.onTabComplete(context);
    }
}
