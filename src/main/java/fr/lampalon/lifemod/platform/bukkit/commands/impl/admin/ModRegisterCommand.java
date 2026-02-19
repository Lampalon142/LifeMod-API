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

public class ModRegisterCommand extends LifeCommand {
    private final LifeMod plugin;

    public ModRegisterCommand(LifeMod plugin) {
        super("modregister", "lifemod.moderator", true);
        this.plugin = plugin;
        setDescription("Register your moderator password.");
        setUsage("/modregister <password>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        ModeratorAuthService authService = plugin.getModeratorAuthService();

        if (authService.isRegistered(player.getUniqueId())) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.already-registered"));
            return;
        }

        String[] args = context.getArgs();
        if (args.length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.register-usage"));
            return;
        }

        String password = args[0];
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "UNKNOWN";

        authService.registerModerator(player.getUniqueId(), player.getName(), password, ip);
        context.getSender().sendMessage(context.getLang().getMessage("commands.auth.registered"));

        if (context.getPlugin().getConfig().getBoolean("modules.discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.modregister.title"))
                    .setDescription(context.getConfig().getString("discord.modregister.description")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.modregister.footer.title"),
                            context.getConfig().getString("discord.modregister.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.modregister.color")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
