package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
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

public class HealCommand extends LifeCommand {
    private final LifeMod plugin;

    public HealCommand(LifeMod plugin) {
        super("heal", "lifemod.heal", true);
        this.plugin = plugin;
        setDescription("Heals a player, restoring their health and hunger.");
        setUsage("/heal [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 0) {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.player"));
            context.getDebug().log("heal", player.getName() + " healed himself");
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target != null) {
                target.setHealth(target.getMaxHealth());
                target.setFoodLevel(20);
                context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.mod", "%player%", target.getName()));
                target.sendMessage(context.getLang().getMessage("commands.utility.heal.player"));
                context.getDebug().log("heal", player.getName() + " healed " + target.getName());
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            }
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.utility.heal.usage"));
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("target_self", context.getArgs().length == 0);
            ph.capture("lifemod_heal", props);
        }

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.heal.title", ""))
                    .setDescription(context.getConfig().getString("discord.heal.description", "").replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.heal.footer.title", ""),
                            context.getConfig().getString("discord.heal.footer.logo", "").replace("%player%", context.getSender().getName()))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.heal.color", "")))));
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
