package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.webhook.WebhookEmbed;
import fr.lampalon.lifemod.common.model.webhook.WebhookFooter;
import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import fr.lampalon.lifemod.common.service.IWebhookService;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffModeManager;
import org.bukkit.entity.Player;

import java.awt.*;

public class ModCommand extends LifeCommand {
    private final StaffModeManager staffModeManager;
    private final LifeMod plugin; // Keep plugin reference for webhook URL

    public ModCommand(LifeMod plugin) { // Constructor now takes LifeMod
        super("mod", "lifemod.mod", true, "staff");
        this.plugin = plugin;
        this.staffModeManager = plugin.getStaffModeManager(); // Access manager from plugin
        setDescription("Toggles staff mode on or off.");
        setUsage("/mod");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (staffModeManager.isMod(player)) {
            staffModeManager.disableStaffMode(player);
        } else {
            staffModeManager.enableStaffMode(player);
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        IWebhookService webhook = ServiceRegistry.get(IWebhookService.class);
        if (webhook == null || !webhook.isEnabled()) return;
        String playerName = context.getSender().getName();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(context.getConfig().getString("modules.discord.alerts.mod.title", ""))
                        .setDescription(context.getConfig().getString("modules.discord.alerts.mod.description", "")
                                .replace("%player%", playerName))
                        .setFooter(new WebhookFooter(
                                context.getConfig().getString("modules.discord.alerts.footer.text", ""),
                                context.getConfig().getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(Color.decode(context.getConfig().getString("modules.discord.alerts.mod.color", "#FF0000")).getRGB())
                        .build())
                .build()).whenComplete((result, error) -> {
                    if (error != null) {
                        context.getDebug().log("discord", "Webhook error: " + error.getMessage());
                    }
                });
    }
}
