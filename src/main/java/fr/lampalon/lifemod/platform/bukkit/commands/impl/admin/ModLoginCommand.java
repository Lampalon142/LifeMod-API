package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorSessionManager;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class ModLoginCommand extends LifeCommand {

    public ModLoginCommand() {
        super("modlogin", "lifemod.moderator", true);
        setDescription("Authenticate as a moderator.");
        setUsage("/modlogin <password>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        ModeratorSessionManager sessionManager = context.getPlugin().getModeratorSessionManager();
        ModeratorAuthService authService = context.getPlugin().getModeratorAuthService();

        if (sessionManager.isAuthenticated(player.getUniqueId())) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.already-authenticated"));
            return;
        }

        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-usage"));
            return;
        }

        String password = context.getArgs()[0];

        if (authService.checkPassword(player.getUniqueId(), password)) {
            sessionManager.authenticate(player.getUniqueId());
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-success"));
            if (context.getPlugin().getConfigConfig().getBoolean("modules.discord.enabled")) {
                sendDiscordAlert(context, true);
            }
            context.getPlugin().getLogger().info("=== LANG DEBUG ===");
            context.getPlugin().getLogger().info("Raw from langConfig: " + context.getPlugin().getLangConfig().getString("commands.auth.login-success"));
            context.getPlugin().getLogger().info("Via ILangService: " + context.getLang().getMessage("commands.auth.login-success"));
            context.getPlugin().getLogger().info("==================");
        } else {
            int attemptsLeft = sessionManager.decrementAttempts(player.getUniqueId());
            if (attemptsLeft <= 0) {
                sessionManager.lock(player.getUniqueId());
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-locked"));
                if (context.getPlugin().getConfigConfig().getBoolean("modules.discord.enabled")) {
                    sendDiscordAlert(context, false);
                }
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-failed", "%attempts%", String.valueOf(attemptsLeft)));
            }
        }
    }

    private void sendDiscordAlert(CommandContext context, boolean success) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.modlogin.title", ""))
                    .setDescription(context.getConfig().getString("discord.modlogin.description", "")
                            .replace("%player%", context.getSender().getName())
                            .replace("%status%", success ? "réussi" : "échoué"))
                    .setFooter(context.getConfig().getString("discord.modlogin.footer.title", ""),
                            context.getConfig().getString("discord.modlogin.footer.logo", ""))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.modlogin.color", "#FFFFFF")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
