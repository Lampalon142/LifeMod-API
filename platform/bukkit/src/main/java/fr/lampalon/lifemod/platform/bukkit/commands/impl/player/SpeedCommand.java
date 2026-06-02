package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.DiscordWebhook;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SpeedCommand extends LifeCommand {
    private final LifeMod plugin; // Keep plugin reference for webhook URL

    public SpeedCommand(LifeMod plugin) { // Constructor now takes LifeMod
        super("speed", "speed.use", true);
        this.plugin = plugin;
        setDescription("Sets the walk or fly speed of the player.");
        setUsage("/speed <1-10>");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        int speed;
        try {
            speed = Integer.parseInt(context.getArgs()[0]);
        } catch (NumberFormatException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        if (speed < 1 || speed > 10) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        if (player.isFlying()) {
            player.setFlySpeed((float) speed / 10);
        } else {
            player.setWalkSpeed((float) speed / 10);
        }

        context.getSender().sendMessage(context.getLang().getMessage("commands.speed.success", "%speed%", String.valueOf(speed)));
        context.getDebug().log("speed", player.getName() + " changed speed to " + speed);

        if (context.getPlugin().getConfig().getBoolean("discord.enabled")) {
            sendDiscordAlert(context);
        }
    }

    private void sendDiscordAlert(CommandContext context) {
        try {
            DiscordWebhook webhook = new DiscordWebhook(context.getPlugin().webHookUrl); 
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(context.getConfig().getString("discord.speed.title", ""))
                    .setDescription(context.getConfig().getString("discord.speed.description", "").replace("%player%", context.getSender().getName()))
                    .setFooter(context.getConfig().getString("discord.speed.footer.title", ""),
                            context.getConfig().getString("discord.speed.footer.logo", "").replace("%player%", context.getSender().getName()))
                    .setColor(Color.decode(Objects.requireNonNull(context.getConfig().getString("discord.speed.color", "")))));
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
            return TabCompleterUtils.filter(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
