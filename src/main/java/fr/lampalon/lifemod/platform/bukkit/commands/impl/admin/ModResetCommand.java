package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ModResetCommand extends LifeCommand {

    public ModResetCommand() {
        super("modreset", "lifemod.admin", false);
        setDescription("Reset a moderator's password (admin only).");
        setUsage("/modreset <player>");
    }

    @Override
    public void execute(CommandContext context) {
        ModeratorAuthService authService = context.getPlugin().getModeratorAuthService();

        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.reset-usage"));
            return;
        }

        String targetName = context.getArgs()[0];
        UUID targetUUID = authService.getUUIDByName(targetName);

        if (targetUUID == null || !authService.isRegistered(targetUUID)) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        authService.resetModeratorPassword(targetUUID);

        context.getSender().sendMessage(context.getLang().getMessage("commands.auth.reset-success", "%player%", targetName));

        if (context.getPlugin().getConfig().getBoolean("modules.discord.enabled")) {
            sendDiscordAlert(context, targetName);
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.modreset.title", ""))
                    .setDescription(context.getConfig().getString("discord.modreset.description", "")
                            .replace("%player%", context.getSender().getName())
                            .replace("%target%", targetName))
                    .setFooter(context.getConfig().getString("discord.modreset.footer.title", ""),
                            context.getConfig().getString("discord.modreset.footer.logo", ""))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.modreset.color", "")))));
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
            return TabCompleterUtils.filter(context.getPlugin().getModeratorAuthService().getAllRegisteredNames(), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
