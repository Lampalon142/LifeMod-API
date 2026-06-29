package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class SpeedCommand extends LifeCommand {
    public SpeedCommand() {
        super("speed", "speed.use", true);
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

        int argIndex = 0;
        boolean fly = player.isFlying();
        boolean walk = false;

        if (context.getArgs()[0].equalsIgnoreCase("fly")) {
            fly = true;
            walk = false;
            argIndex = 1;
        } else if (context.getArgs()[0].equalsIgnoreCase("walk")) {
            walk = true;
            fly = false;
            argIndex = 1;
        }

        if (argIndex >= context.getArgs().length) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        int speed;
        try {
            speed = Integer.parseInt(context.getArgs()[argIndex]);
        } catch (NumberFormatException e) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        if (speed < 1 || speed > 10) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.speed.provide"));
            return;
        }

        float value = (float) speed / 10;
        if (fly) {
            player.setFlySpeed(value);
        } else {
            player.setWalkSpeed(value);
        }

        context.getSender().sendMessage(context.getLang().getMessage("commands.speed.success", "%speed%", String.valueOf(speed)));
        context.getDebug().log("speed", player.getName() + " changed speed to " + speed);

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context);
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filter(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "fly", "walk"), context.getArgs()[0]);
        }
        if (context.getArgs().length == 2 && (context.getArgs()[0].equalsIgnoreCase("fly") || context.getArgs()[0].equalsIgnoreCase("walk"))) {
            return TabCompleterUtils.filter(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), context.getArgs()[1]);
        }
        return super.onTabComplete(context);
    }
}
