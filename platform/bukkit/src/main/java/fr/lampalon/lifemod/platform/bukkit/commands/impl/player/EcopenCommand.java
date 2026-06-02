package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class EcopenCommand extends LifeCommand {
    private final LifeMod plugin;

    public EcopenCommand(LifeMod plugin) {
        super("ecopen", "lifemod.ecopen", true);
        this.plugin = plugin;
        setDescription("Opens the Ender Chest of another player.");
        setUsage("/ecopen <player>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.ec.usage"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        if (targetPlayer == player) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.ec.yourself"));
            return;
        }

        player.openInventory(targetPlayer.getEnderChest());
        context.getDebug().log("ecopen", player.getName() + " opened ender chest of " + targetPlayer.getName());

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, targetPlayer.getName());
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.ecopen.title", ""))
                    .setDescription(context.getConfig().getString("discord.ecopen.description", "")
                            .replace("%player%", context.getSender().getName())
                            .replace("%target%", targetName))
                    .setFooter(context.getConfig().getString("discord.ecopen.footer.title", ""),
                            context.getConfig().getString("discord.ecopen.footer.logo", ""))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.ecopen.color", "")))));
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
