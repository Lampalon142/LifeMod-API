package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

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

public class ClearinvCommand extends LifeCommand {
    private final LifeMod plugin;

    public ClearinvCommand(LifeMod plugin) {
        super("clearinv", "lifemod.clearinv", true);
        this.plugin = plugin;
        setDescription("Clears the inventory of a player.");
        setUsage("/clearinv <player>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.clearinv.usage"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(context.getArgs()[0]);
        if (targetPlayer == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        targetPlayer.getInventory().clear();
        context.getSender().sendMessage(context.getLang().getMessage("commands.clearinv.message", "%target%", targetPlayer.getName()));
        context.getDebug().log("clearinv", player.getName() + " cleared inventory of " + targetPlayer.getName());

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.clearinv.title", ""))
                    .setDescription(context.getConfig().getString("discord.clearinv.description", "")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(
                            context.getConfig().getString("discord.clearinv.footer.title", ""),
                            context.getConfig().getString("discord.clearinv.footer.logo", "")
                                    .replace("%player%", context.getSender().getName())
                    )
                    .setColor(Color.decode(Objects.requireNonNull(
                            context.getConfig().getString("discord.clearinv.color", "")
                    ))));
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
