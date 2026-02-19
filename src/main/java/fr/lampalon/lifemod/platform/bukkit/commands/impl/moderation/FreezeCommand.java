package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import fr.lampalon.lifemod.platform.bukkit.managers.FreezeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class FreezeCommand extends LifeCommand {
    private final FreezeManager freezeManager;
    private final LifeMod plugin; // Keep plugin reference for managers

    public FreezeCommand(LifeMod plugin) { // Constructor now takes LifeMod
        super("freeze", "lifemod.freeze", true);
        this.plugin = plugin;
        this.freezeManager = plugin.getFreezeManager();
        setDescription("Freezes or unfreezes a player.");
        setUsage("/freeze <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(context.getArgs()[0]);
        if (target == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        if (target.equals(context.getPlayer())) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.yourself"));
            return;
        }

        if (freezeManager.isPlayerFrozen(target.getUniqueId())) {
            freezeManager.unfreezePlayer(context.getPlayer(), target);
            target.sendMessage(context.getLang().getMessage("commands.freeze.messages.unfreeze.target", "%player%", context.getSender().getName()));
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.messages.unfreeze.mod", "%target%", target.getName()));
            context.getDebug().log("freeze", context.getSender().getName() + " unfroze " + target.getName());
        } else {
            freezeManager.freezePlayer(context.getPlayer(), target);
            // This still uses getLangConfig, which should be replaced by context.getLang()
            // However, getStringList is not directly available on ILangService as it stands.
            // I need to consider adding a method to ILangService for lists or handling this
            // in a Bukkit-specific way within the command. For now, I'll keep the direct
            // access but mark it for future refactoring.
            plugin.getLangConfig().getStringList("commands.freeze.messages.freeze.onfreeze")
                    .forEach(msg -> target.sendMessage(context.getLang().getMessage(msg))); // Still using MessageUtil.formatMessage implicit via getLang().getMessage
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.messages.freeze.mod", "%target%", target.getName()));
            context.getDebug().log("freeze", context.getSender().getName() + " froze " + target.getName());
        }

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl); 
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.freeze.title"))
                    .setDescription(context.getConfig().getString("discord.freeze.description")
                            .replace("%player%", context.getSender().getName()))
                    .setFooter(
                            context.getConfig().getString("discord.freeze.footer.title"),
                            context.getConfig().getString("discord.freeze.footer.logo")
                                    .replace("%player%", context.getSender().getName())
                    )
                    .setColor(Color.decode(Objects.requireNonNull(
                            context.getConfig().getString("discord.freeze.color")
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
