package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FeedCommand extends LifeCommand {

    public FeedCommand() {
        super("feed", "lifemod.feed", true);
        setDescription("Feeds a player, restoring their hunger.");
        setUsage("/feed [player]");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length == 0) {
            context.getPlayer().setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.yourself"));
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }
            target.setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.mod", "%target%", target.getName()));
            target.sendMessage(context.getLang().getMessage("commands.utility.feed.player"));
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.feed.usage"));
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("target_self", context.getArgs().length == 0);
            ph.capture("lifemod_feed", props);
        }

        if (context.getPlugin().getConfigConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.feed.title", ""))
                    .setDescription(context.getConfig().getString("discord.feed.description", "")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(
                            context.getConfig().getString("discord.feed.footer.title", ""),
                            context.getConfig().getString("discord.feed.footer.logo", "")
                                    .replace("%player%", context.getSender().getName())
                    )
                    .setColor(Color.decode(Objects.requireNonNull(
                            context.getConfig().getString("discord.feed.color", "")
                    ))));
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
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
