package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HeartsCommand extends LifeCommand {
    private final LifeMod plugin;
    private static final double MAX_HEARTS = 20.0;

    public HeartsCommand(LifeMod plugin) {
        super("hearts", "lifemod.hearts", false);
        this.plugin = plugin;
        setDescription("Manage player hearts: set or add a specific amount of hearts to any player.");
        setUsage("/hearts <set|add> <player> <amount>");
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.getArgs();

        if (args.length < 3) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.usage"));
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.unit-info"));
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        double hearts;
        try {
            hearts = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.invalid-amount"));
            return;
        }

        hearts = Math.round(hearts * 2.0) / 2.0; // Ensure half-heart increments

        if (hearts <= 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.invalid-amount"));
            return;
        }

        if (hearts > MAX_HEARTS) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.max-reached"));
            return;
        }

        double healthPoints = hearts * 2.0;
        double newMaxHealth;
        if ("set".equals(action)) {
            newMaxHealth = healthPoints;
        } else if ("add".equals(action)) {
            newMaxHealth = Math.min(target.getMaxHealth() + healthPoints, MAX_HEARTS * 2.0);
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.usage"));
            return;
        }

        newMaxHealth = Math.round(newMaxHealth * 2.0) / 2.0;

        if (newMaxHealth > MAX_HEARTS * 2.0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.max-reached"));
            return;
        }

        if (newMaxHealth % 1 != 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.half-heart-warning"));
        }

        target.setMaxHealth(newMaxHealth);
        target.setHealth(Math.min(target.getHealth(), newMaxHealth));

        context.getSender().sendMessage(context.getLang().getMessage("commands.hearts.success",
                "%action%", action,
                "%target%", target.getName(),
                "%amount%", String.valueOf(hearts)));

        target.sendMessage(context.getLang().getMessage("commands.hearts.changed", "%amount%", String.valueOf(newMaxHealth / 2.0)));

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context, target.getName(), action, hearts);
        }
    }

    private void sendDiscordAlert(CommandContext context, String targetName, String action, double amount) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl);
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.hearts.title"))
                    .setDescription(context.getConfig().getString("discord.hearts.description")
                            .replace("%player%", context.getSender().getName())
                            .replace("%target%", targetName)
                            .replace("%action%", action)
                            .replace("%amount%", String.valueOf(amount)))
                    .setFooter(context.getConfig().getString("discord.hearts.footer.title"),
                            context.getConfig().getString("discord.hearts.footer.logo"))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.hearts.color")))));
            webhook.execute();
        } catch (IOException e) {
            context.getDebug().log("discord", "Webhook error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 1) {
            return filter(Arrays.asList("set", "add"), context);
        }
        if (args.length == 2) {
            return TabCompleterUtils.filterOnlinePlayers(args[1]);
        }
        return super.onTabComplete(context);
    }
}
