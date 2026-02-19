package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.database.DatabaseManager;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.utils.BukkitDatabaseUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class OInvseeCommand extends LifeCommand {
    private final LifeMod plugin;
    private final DatabaseManager databaseManager;

    public OInvseeCommand(LifeMod plugin, DatabaseManager databaseManager) {
        super("oinvsee", "lifemod.oinvsee", true);
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        setDescription("View the inventory of an offline player.");
        setUsage("/oinvsee <player>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        String serverName = plugin.getServerName();
        ItemStack[] savedInventory = BukkitDatabaseUtil.deserializeInventory(databaseManager.getDatabaseProvider().getRawInventory(target.getUniqueId(), serverName));
        if (savedInventory == null) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.no-inventory", "%target%", args[0]));
            return;
        }

        String inventoryTitle = context.getLang().getMessage("commands.oinvsee.name", "%target%", args[0]);
        Inventory inv = Bukkit.createInventory(null, 45, inventoryTitle);
        inv.setContents(savedInventory);
        player.openInventory(inv);

        context.getSender().sendMessage(context.getLang().getMessage("commands.oinvsee.success", "%target%", args[0]));

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, args[0]);
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.oinvsee.title"))
                    .setDescription(context.getConfig().getString("discord.oinvsee.description")
                            .replace("%player%", context.getSender().getName())
                            .replace("%target%", targetName))
                    .setFooter(context.getConfig().getString("discord.oinvsee.footer.title"),
                            context.getConfig().getString("discord.oinvsee.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.oinvsee.color")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
