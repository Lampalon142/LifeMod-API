package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Inventory inv = createOnlineInventory(player, onlineTarget, context);
            plugin.getInvseeManager().startViewing(player, onlineTarget);
            player.openInventory(inv);
            player.sendMessage(context.getLang().getMessage("commands.invsee.opened",
                "%target%", onlineTarget.getName(),
                "%status%", "§a(Online)"));
            context.getDebug().log("invsee", player.getName() + " opened online inventory of " + onlineTarget.getName());
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);
            if (!offlineTarget.hasPlayedBefore()) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }
            String serverName = plugin.getServerName();
            ItemStack[] savedInventory = BukkitDatabaseUtil.deserializeInventory(
                plugin.getDatabaseManager().getDatabaseProvider().getRawInventory(offlineTarget.getUniqueId(), serverName));
            if (savedInventory == null) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.no-inventory", "%target%", args[0]));
                return;
            }
            String inventoryTitle = context.getLang().getMessage("commands.oinvsee.name", "%target%", args[0]);
            Inventory inv = Bukkit.createInventory(null, 45, inventoryTitle);
            inv.setContents(savedInventory);
            player.openInventory(inv);
            player.sendMessage(context.getLang().getMessage("commands.invsee.opened",
                "%target%", args[0],
                "%status%", "§c(Offline)"));
            context.getDebug().log("invsee", player.getName() + " opened offline inventory of " + args[0]);
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("interaction", "view");
            props.put("inventory_type", "player");
            ph.capture("lifemod_invsee", props);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, args[0]);
        }
    }

    private Inventory createOnlineInventory(Player viewer, Player target, CommandContext context) {
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

    private void sendDiscordAlert(CommandContext context, String targetName) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%target%", targetName))
                        .setFooter(new WebhookFooter(
                                context.getConfig().getString("modules.discord.alerts.footer.text", ""),
                                context.getConfig().getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(Color.decode(context.getConfig().getString("modules.discord.alerts.generic.color", "#FF0000")).getRGB())
                        .build())
                .build()).whenComplete((result, error) -> {
                    if (error != null) {
                        context.getDebug().log("discord", "Webhook error: " + error.getMessage());
                    }
                });
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
