package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import org.bukkit.entity.Player;

/**
 * Command for initiating a replay playback.
 */
public class ReplayCommand extends LifeCommand {

    private final LifeMod plugin;

    public ReplayCommand(LifeMod plugin) {
        super("replay", "replay.use", false, new String[0]);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandContext context) {
        if (!(context.getSender() instanceof Player)) {
            context.getSender().sendMessage("§cOnly players can use this command.");
            return;
        }

        if (context.getArgs().length < 2) {
            context.getSender().sendMessage("§cUsage: /replay <player> <time>");
            return;
        }

        String targetName = context.getArgs()[0];
        String timeStr = context.getArgs()[1];

        long duration = parseTime(timeStr);
        if (duration == -1) {
            context.getSender().sendMessage("§cInvalid time format. Use: 1h, 30m, 10s.");
            return;
        }

        context.getSender().sendMessage("§aStarting playback for " + targetName + " (" + timeStr + ")...");
        Player mod = (Player) context.getSender();
        mod.setGameMode(org.bukkit.GameMode.SPECTATOR);
        mod.getInventory().clear();
        new fr.lampalon.lifemod.platform.bukkit.replay.gui.ReplayGui().open(mod);
    }

    private long parseTime(String timeStr) {
        try {
            if (timeStr.endsWith("h")) return Long.parseLong(timeStr.replace("h", "")) * 3600000L;
            if (timeStr.endsWith("m")) return Long.parseLong(timeStr.replace("m", "")) * 60000L;
            if (timeStr.endsWith("s")) return Long.parseLong(timeStr.replace("s", "")) * 1000L;
        } catch (NumberFormatException e) {
            return -1;
        }
        return -1;
    }
}
