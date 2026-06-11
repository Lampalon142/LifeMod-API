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
    private final LifeMod plugin;
    private final int cachedColor;

    public ModCommand(LifeMod plugin) {
        super("mod", "lifemod.mod", true, "staff");
        this.plugin = plugin;
        this.staffModeManager = plugin.getStaffModeManager();
        this.cachedColor = parseColor(plugin.getConfigConfig().getString("modules.discord.alerts.mod.color", "#FF0000"));
        setDescription("Toggles staff mode on or off.");
        setUsage("/mod");
    }

    private static int parseColor(String hex) {
        try {
            return Color.decode(hex).getRGB() & 0xFFFFFF;
        } catch (Exception e) {
            return 0xFF0000;
        }
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
        var cfg = context.getConfig();
        webhook.send(new WebhookMessage.Builder()
                .addEmbed(new WebhookEmbed.Builder()
                        .setTitle(cfg.getString("modules.discord.alerts.mod.title", ""))
                        .setDescription(cfg.getString("modules.discord.alerts.mod.description", "")
                                .replace("%player%", playerName))
                        .setFooter(new WebhookFooter(
                                cfg.getString("modules.discord.alerts.footer.text", ""),
                                cfg.getString("modules.discord.alerts.footer.logo", "")
                                        .replace("%player%", playerName)))
                        .setColor(cachedColor)
                        .build())
                .build());
    }
}
