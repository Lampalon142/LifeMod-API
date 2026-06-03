package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChatclearCommand extends LifeCommand {

    public ChatclearCommand() {
        super("chatclear", "lifemod.chatclear", false);
        setDescription("Clears the chat for all players.");
        setUsage("/chatclear");
    }

    @Override
    public void execute(CommandContext context) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage("");
            }
            player.sendMessage(context.getLang().getMessage("commands.chatclear.message"));
        }
        context.getDebug().log("chatclear", "Chat cleared by " + context.getSender().getName());

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            ph.capture("lifemod_chat_clear", new HashMap<>());
        }

        if (context.getPlugin().getConfigConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.chatclear.title", ""))
                    .setDescription(context.getConfig().getString("discord.chatclear.description", "")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(
                            context.getConfig().getString("discord.chatclear.footer.title", ""),
                            context.getConfig().getString("discord.chatclear.footer.logo", "")
                                    .replace("%player%", context.getSender().getName())
                    )
                    .setColor(Color.decode(Objects.requireNonNull(
                            context.getConfig().getString("discord.chatclear.color", "")
                    ))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
