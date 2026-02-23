package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class ModChangePassCommand extends LifeCommand {

    public ModChangePassCommand() {
        super("modchangepass", "lifemod.moderator", true);
        setDescription("Change your moderator password.");
        setUsage("/modchangepass <oldPassword> <newPassword>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        ModeratorAuthService authService = context.getPlugin().getModeratorAuthService();

        if (context.getArgs().length != 2) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.changepass-usage"));
            return;
        }

        String oldPass = context.getArgs()[0];
        String newPass = context.getArgs()[1];

        if (!authService.checkPassword(player.getUniqueId(), oldPass)) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.incorrect-old-password"));
            return;
        }

        authService.changePassword(player.getUniqueId(), newPass);
        context.getSender().sendMessage(context.getLang().getMessage("commands.auth.password-changed"));

        if (context.getPlugin().getConfigConfig().getBoolean("modules.discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.modchangepass.title", ""))
                    .setDescription(context.getConfig().getString("discord.modchangepass.description", "")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.modchangepass.footer.title", ""),
                            context.getConfig().getString("discord.modchangepass.footer.logo", ""))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.modchangepass.color", "#FFFFFF")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
