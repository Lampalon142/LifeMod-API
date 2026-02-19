package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class ModChangePassCommand extends LifeCommand {
    private final LifeMod plugin;

    public ModChangePassCommand(LifeMod plugin) {
        super("modchangepass", "lifemod.moderator", true);
        this.plugin = plugin;
        setDescription("Change your moderator password.");
        setUsage("/modchangepass <oldPassword> <newPassword>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        ModeratorAuthService authService = plugin.getModeratorAuthService();
        String[] args = context.getArgs();

        if (args.length != 2) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.changepass-usage"));
            return;
        }

        String oldPass = args[0];
        String newPass = args[1];

        if (!authService.checkPassword(player.getUniqueId(), oldPass)) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.incorrect-old-password"));
            return;
        }

        authService.changePassword(player.getUniqueId(), newPass);
        context.getSender().sendMessage(context.getLang().getMessage("commands.auth.password-changed"));

        if (context.getPlugin().getConfig().getBoolean("modules.discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.modchangepass.title"))
                    .setDescription(context.getConfig().getString("discord.modchangepass.description")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.modchangepass.footer.title"),
                            context.getConfig().getString("discord.modchangepass.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.modchangepass.color")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
