package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IItemsAdderService;
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
import java.util.List;
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
            context.getSender().sendMessage("§cUsage: " + getUsage());
            return;
        }

        String type = context.getArgs()[0];
        String targetStr = context.getArgs()[1];
        String itemStr = context.getArgs()[2];

        ItemStack targetItem = null;
        if (itemStr.equalsIgnoreCase("hand")) {
            targetItem = context.getPlayer().getInventory().getItemInMainHand();
            if (targetItem == null || targetItem.getType() == Material.AIR) {
                context.getSender().sendMessage("§cVous devez tenir un item en main.");
                return;
            }
        } else {
            // Try ItemsAdder first
            if (itemsAdderService != null && itemsAdderService.isEnabled()) {
                targetItem = itemsAdderService.getItem(itemStr);
            }
            
            // Try Bukkit Material if not found or IA not enabled
            if (targetItem == null) {
                Material material = Material.matchMaterial(itemStr);
                if (material != null) {
                    targetItem = new ItemStack(material);
                }
            }
        }

        if (targetItem == null) {
            context.getSender().sendMessage("§cItem invalide : " + itemStr);
            return;
        }

        String itemName = targetItem.getType().name();
        if (itemsAdderService != null && itemsAdderService.isItemsAdderItem(targetItem)) {
            itemName = itemsAdderService.getItemId(targetItem);
        }

        context.getSender().sendMessage("§6§lLifeMod §8» §7Lancement du scan pour §e" + itemName + "§7...");

        plugin.getScanManager().scan(type, targetStr, targetItem, progress -> {
            context.getSender().sendMessage("§6§lScan §8» " + progress);
        }).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> sendReport(context.getSender(), result));
        });
    }

    private void sendReport(org.bukkit.command.CommandSender sender, ScanResult result) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§6§lRapport de Scan");
        sender.sendMessage(" ");
        sender.sendMessage("§e• §fTotal trouvé: §b" + result.getTotalCount());
        sender.sendMessage("§e• §fDurée: §b" + result.getDuration() + "ms");
        
        if (!result.getFoundPlayers().isEmpty()) {
            sender.sendMessage(" ");
            sender.sendMessage("§e§lJoueurs :");
            result.getFoundPlayers().forEach((uuid, count) -> {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                sender.sendMessage("  §7- §f" + name + " : §e" + count);
            });
        }

        if (!result.getFoundLocations().isEmpty()) {
            sender.sendMessage(" ");
            sender.sendMessage("§e§lConteneurs :");
            int limit = 10;
            int count = 0;
            for (var entry : result.getFoundLocations().entrySet()) {
                if (count >= limit) {
                    sender.sendMessage("  §7... et §e" + (result.getFoundLocations().size() - limit) + " §7autres positions.");
                    break;
                }
                Location loc = entry.getKey();
                sender.sendMessage("  §7- §f" + loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " : §e" + entry.getValue());
                count++;
            }
        }

        sender.sendMessage("§8§m----------------------------------------");
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
