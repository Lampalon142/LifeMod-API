package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.StaffModeManager;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

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

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl); 
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.mod.title"))
                    .setDescription(context.getConfig().getString("discord.mod.description").replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.mod.footer.title"),
                            context.getConfig().getString("discord.mod.footer.logo").replace("%player%", context.getSender().getName()))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.mod.color")))));
            webhook.execute();
        } catch (Exception e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        }
    }
}
