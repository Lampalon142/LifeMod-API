package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.util.List;

public class OInvseeCommand extends LifeCommand {

    public OInvseeCommand() {
        super("oinvsee", "lifemod.oinvsee", true);
        setDescription("View the inventory of an offline player.");
        setUsage("/oinvsee <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(context.getArgs()[0]);
        if (!target.hasPlayedBefore()) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        String serverName = context.getPlugin().getServerName();
        ItemStack[] savedInventory = BukkitDatabaseUtil.deserializeInventory(context.getPlugin().getDatabaseManager().getDatabaseProvider().getRawInventory(target.getUniqueId(), serverName));
        if (savedInventory == null) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.no-inventory", "%target%", context.getArgs()[0]));
            return;
        }

        String inventoryTitle = context.getLang().getMessage("commands.oinvsee.name", "%target%", context.getArgs()[0]);
        Inventory inv = Bukkit.createInventory(null, 45, inventoryTitle);
        inv.setContents(savedInventory);
        context.getPlayer().openInventory(inv);

        context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.success", "%target%", context.getArgs()[0]));

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, context.getArgs()[0]);
        }
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
