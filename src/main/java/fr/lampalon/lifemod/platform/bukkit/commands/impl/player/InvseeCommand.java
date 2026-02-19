package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class InvseeCommand extends LifeCommand {
    private final LifeMod plugin;

    public InvseeCommand(LifeMod plugin) {
        super("invsee", "lifemod.invsee", true);
        this.plugin = plugin;
        setDescription("Views the inventory of another player.");
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

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        Inventory inv = createTargetInventory(player, targetPlayer, context);
        plugin.getInvseeManager().startViewing(player, targetPlayer);
        player.openInventory(inv);

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, targetPlayer.getName());
        }
    }

    private Inventory createTargetInventory(Player viewer, Player target, CommandContext context) {
        String invTitle = context.getLang().getMessage("commands.invsee.name", "%player%", target.getName());
        Inventory targetInventory = Bukkit.createInventory(null, 45, invTitle);
        PlayerInventory targetPlayerInventory = target.getInventory();

        // Main inventory
        for (int i = 0; i < 36; i++) {
            ItemStack item = targetPlayerInventory.getItem(i);
            if (item != null) {
                targetInventory.setItem(i, item);
            }
        }
        // Armor slots
        targetInventory.setItem(36, targetPlayerInventory.getHelmet());
        targetInventory.setItem(37, targetPlayerInventory.getChestplate());
        targetInventory.setItem(38, targetPlayerInventory.getLeggings());
        targetInventory.setItem(39, targetPlayerInventory.getBoots());

        context.getDebug().log("invsee", viewer.getName() + " opened inventory of " + target.getName());
        return targetInventory;
    }

    private void sendDiscordAlert(CommandContext context, String targetName) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.invsee.title"))
                    .setDescription(context.getConfig().getString("discord.invsee.description")
                            .replace("%player%", context.getSender().getName())
                            .replace("%target%", targetName))
                    .setFooter(context.getConfig().getString("discord.invsee.footer.title"),
                            context.getConfig().getString("discord.invsee.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.invsee.color")))));
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
