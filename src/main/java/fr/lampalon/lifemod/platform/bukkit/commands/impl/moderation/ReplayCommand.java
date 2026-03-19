package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.replay.PlaybackManager;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Command for initiating or stopping a replay playback.
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

        Player mod = (Player) context.getSender();
        ReplayPlayerManager rpm = plugin.getReplayPlayerManager();
        
        if (context.getArgs().length < 1) {
            mod.sendMessage("§cUsage: /replay <player> [duration] or /replay stop/exit");
            mod.sendMessage("§7Duration example: 1m, 2.5m, 10s (max 1h)");
            return;
        }

        String targetOrAction = context.getArgs()[0];

        if (targetOrAction.equalsIgnoreCase("stop") || targetOrAction.equalsIgnoreCase("exit")) {
            if (!rpm.isInReplay(mod)) {
                mod.sendMessage("§cYou are not in a replay session.");
                return;
            }
            rpm.exitReplay(mod);
            mod.sendMessage("§aReplay session stopped. Your state has been restored.");
            return;
        }

        Player target = Bukkit.getPlayer(targetOrAction);
        if (target == null) {
            mod.sendMessage("§cPlayer not found.");
            return;
        }

        ReplaySession session = plugin.getReplayManager().getSession(target.getUniqueId());
        if (session == null) {
            mod.sendMessage("§cNo active recording for §e" + target.getName());
            return;
        }

        long durationMs = 60000; // Default: 1 minute
        if (context.getArgs().length >= 2) {
            try {
                durationMs = parseDuration(context.getArgs()[1]);
            } catch (IllegalArgumentException e) {
                mod.sendMessage("§cInvalid duration format. Use e.g. 5m, 10s. Max 1h.");
                return;
            }
        }

        List<ReplayFrame> frames = session.getRecordedData(durationMs);
        mod.sendMessage("§aFound §e" + frames.size() + "§a frames in the last §e" + (durationMs / 1000.0) + "s.");
        
        if (frames.isEmpty()) {
            mod.sendMessage("§cNo data found for the requested duration.");
            return;
        }

        mod.sendMessage("§aLoading replay for §e" + target.getName() + "§a...");
        
        rpm.enterReplay(mod);
        
        // Start playback using PlaybackManager
        PlaybackManager playbackManager = new PlaybackManager(plugin);
        playbackManager.startPlayback(mod, frames, session);
    }

    private long parseDuration(String input) {
        input = input.toLowerCase();
        double value;
        if (input.endsWith("m")) {
            value = Double.parseDouble(input.replace("m", ""));
            return (long) (value * 60000);
        } else if (input.endsWith("s")) {
            value = Double.parseDouble(input.replace("s", ""));
            return (long) (value * 1000);
        } else if (input.endsWith("h")) {
            value = Double.parseDouble(input.replace("h", ""));
            return (long) (value * 3600000);
        } else {
            // Assume minutes if no unit
            value = Double.parseDouble(input);
            return (long) (value * 60000);
        }
    }
}
