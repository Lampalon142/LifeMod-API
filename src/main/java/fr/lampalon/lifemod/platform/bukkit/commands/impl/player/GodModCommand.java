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

public class GodModCommand extends LifeCommand {
    private final LifeMod plugin;

    public GodModCommand(LifeMod plugin) {
        super("god", "lifemod.god", false);
        this.plugin = plugin;
        setDescription("Toggles invulnerability (god mode) for yourself or another player.");
        setUsage("/god [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player targetPlayer;

        if (context.getArgs().length > 0) {
            targetPlayer = Bukkit.getPlayer(context.getArgs()[0]);
            if (targetPlayer == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }
        } else {
            if (!context.isPlayer()) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }
            targetPlayer = context.getPlayer();
        }

        if (targetPlayer.isInvulnerable()) {
            targetPlayer.setInvulnerable(false);
            targetPlayer.sendMessage(context.getLang().getMessage("commands.utility.god.deactivate.own"));
            if (!targetPlayer.equals(context.getPlayer())) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.utility.god.deactivate.other", "%player%", targetPlayer.getName()));
            }
            context.getDebug().log("god", "God mode disabled for " + targetPlayer.getName() + " by " + context.getSender().getName());
        } else {
            targetPlayer.setInvulnerable(true);
            targetPlayer.sendMessage(context.getLang().getMessage("commands.utility.god.activate.own"));
            if (!targetPlayer.equals(context.getPlayer())) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.utility.god.activate.other", "%player%", targetPlayer.getName()));
            }
            context.getDebug().log("god", "God mode enabled for " + targetPlayer.getName() + " by " + context.getSender().getName());
        }

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, targetPlayer.getName(), targetPlayer.isInvulnerable());
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName, boolean activated) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.god.title"))
                    .setDescription(context.getConfig().getString("discord.god.description")
                            .replace("%player%", context.getSender().getName())
                            .replace("%target%", targetName)
                            .replace("%status%", activated ? "activé" : "désactivé"))
                    .setFooter(context.getConfig().getString("discord.god.footer.title"),
                            context.getConfig().getString("discord.god.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.god.color")))));
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
