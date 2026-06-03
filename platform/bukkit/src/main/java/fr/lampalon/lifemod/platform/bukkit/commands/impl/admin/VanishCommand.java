package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.IVanishService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VanishCommand extends LifeCommand {
    private final LifeMod plugin;
    private final IVanishService vanishService;

    public VanishCommand(LifeMod plugin) {
        super("vanish", "lifemod.vanish", true, "v");
        this.plugin = plugin;
        this.vanishService = plugin.getVanishService();
        setDescription("Toggles player visibility (vanish).");
        setUsage("/vanish [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 0) {
            boolean newState = !vanishService.isVanished(player.getUniqueId());
            vanishService.setVanished(player, newState, false);
            String msgKey = newState ? "vanish.activate" : "vanish.deactivate";
            context.getSender().sendMessage(context.getLang().getMessage(msgKey));
            context.getDebug().log("vanish", player.getName() + " toggled vanish to " + newState);
            trackVanish(newState);
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target != null) {
                boolean newState = !vanishService.isVanished(target.getUniqueId());
                vanishService.setVanished(target, newState, false);
                
                String targetMsgKey = newState ? "vanish.activate" : "vanish.deactivate";
                String senderMsgKey = newState ? "mod.items.vanish-on" : "mod.items.vanish-off";
                
                target.sendMessage(context.getLang().getMessage(targetMsgKey));
                context.getSender().sendMessage(context.getLang().getMessage(senderMsgKey));
                context.getDebug().log("vanish", player.getName() + " toggled vanish for " + target.getName() + " to " + newState);
                trackVanish(newState);
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            }
        }

        if (context.getPlugin().getConfigConfig().getBoolean("modules.discord.enabled", false)) {
            sendDiscordAlert(context);
        }
    }

    private void trackVanish(boolean enabled) {
        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("action", enabled ? "enable" : "disable");
            ph.capture("lifemod_vanish", props);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("modules.discord.vanish.title", ""))
                    .setDescription(context.getConfig().getString("modules.discord.vanish.description", "").replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("modules.discord.vanish.footer.title", ""),
                            context.getConfig().getString("modules.discord.vanish.footer.logo", "").replace("%player%", context.getSender().getName()))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("modules.discord.vanish.color", "#60a5fa")))));
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
