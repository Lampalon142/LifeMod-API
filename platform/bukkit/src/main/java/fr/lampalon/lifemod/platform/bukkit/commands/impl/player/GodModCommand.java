package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GodModCommand extends LifeCommand {

    public GodModCommand() {
        super("god", "lifemod.god", false);
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

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("action", targetPlayer.isInvulnerable() ? "enable" : "disable");
            ph.capture("lifemod_god", props);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context, targetPlayer.getName(), targetPlayer.isInvulnerable());
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName, boolean activated) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.generic.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.generic.description", "")
                                .replace("%player%", playerName)
                                .replace("%target%", targetName)
                                .replace("%status%", activated ? "activé" : "désactivé"))
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
