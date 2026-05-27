package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.replay.ReplaySession;
import fr.lampalon.lifemod.common.replay.packet.ReplayFrame;
import fr.lampalon.lifemod.common.replay.storage.BinaryReplayReader;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.replay.PlaybackManager;
import fr.lampalon.lifemod.platform.bukkit.replay.ReplayPlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
            sendHelp(mod);
            return;
        }

        String action = context.getArgs()[0];

        if (action.equalsIgnoreCase("stop") || action.equalsIgnoreCase("exit")) {
            if (!rpm.isInReplay(mod)) {
                mod.sendMessage("§cYou are not in a replay session.");
                return;
            }
            rpm.exitReplay(mod);
            mod.sendMessage("§aReplay session stopped. Your state has been restored.");
            return;
        }

        if (action.equalsIgnoreCase("list")) {
            listReplays(mod);
            return;
        }

        if (action.equalsIgnoreCase("load")) {
            if (context.getArgs().length < 2) {
                mod.sendMessage("§cUsage: /replay load <sessionName>");
                return;
            }
            loadAndPlay(mod, context.getArgs()[1]);
            return;
        }

        // Default: play from active session
        playFromActive(mod, action, context.getArgs());
    }

    private void sendHelp(Player mod) {
        mod.sendMessage("§6§lLifeMod Replay System");
        mod.sendMessage("§e/replay <player> [duration] §7- Play from active recording");
        mod.sendMessage("§e/replay load <name> §7- Play from saved file");
        mod.sendMessage("§e/replay list §7- List saved replays");
        mod.sendMessage("§e/replay stop §7- Exit current replay");
        mod.sendMessage("§7Duration: 1m, 2.5m, 10s (max 1h)");
    }

    private void listReplays(Player mod) {
        File replayDir = new File("plugins/LifeMod/replays");
        if (!replayDir.exists() || !replayDir.isDirectory()) {
            mod.sendMessage("§cNo replays found.");
            return;
        }
        File[] files = replayDir.listFiles((dir, name) -> name.endsWith(".replay"));
        if (files == null || files.length == 0) {
            mod.sendMessage("§cNo replays found.");
            return;
        }
        mod.sendMessage("§6§lAvailable Replays:");
        for (File f : files) {
            mod.sendMessage("§e- " + f.getName().replace(".replay", "") + " §7(" + (f.length() / 1024) + " KB)");
        }
    }

    private void loadAndPlay(Player mod, String sessionName) {
        File replayFile = new File("plugins/LifeMod/replays", sessionName + ".replay");
        if (!replayFile.exists()) {
            mod.sendMessage("§cReplay file not found: " + sessionName);
            return;
        }

        mod.sendMessage("§aReading replay file...");

        CompletableFuture.supplyAsync(() -> {
            BinaryReplayReader reader = new BinaryReplayReader(replayFile);
            List<ReplayFrame> frames = reader.readAllFrames();
            if (frames.isEmpty()) return null;
            return new Object[] {
                frames,
                reader.getPlayerUUID(),
                reader.getPlayerName(),
                reader.getEntityId(),
                new Location(mod.getWorld(), reader.getStartX(), reader.getStartY(), reader.getStartZ(), reader.getStartYaw(), reader.getStartPitch())
            };
        }).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result == null) {
                    mod.sendMessage("§cReplay file is empty or invalid.");
                    return;
                }
                @SuppressWarnings("unchecked")
                List<ReplayFrame> frames = (List<ReplayFrame>) result[0];
                UUID targetUUID = (UUID) result[1];
                String targetName = (String) result[2];
                int entityId = (int) result[3];
                Location startLoc = (Location) result[4];

                mod.sendMessage("§aLoaded §e" + frames.size() + "§a frames for §e" + targetName);
                plugin.getReplayPlayerManager().enterReplay(mod);
                PlaybackManager playbackManager = new PlaybackManager(plugin);
                playbackManager.startPlayback(mod, frames, entityId, targetUUID, targetName, startLoc);
            });
        });
    }

    private void playFromActive(Player mod, String targetName, String[] args) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            mod.sendMessage("§cPlayer not found: " + targetName);
            return;
        }

        ReplaySession session = plugin.getReplayManager().getSession(target.getUniqueId());
        if (session == null) {
            mod.sendMessage("§cNo active recording for §e" + target.getName());
            return;
        }

        long durationMs = 60000; // Default: 1 minute
        if (args.length >= 2) {
            try {
                durationMs = parseDuration(args[1]);
            } catch (IllegalArgumentException e) {
                mod.sendMessage("§cInvalid duration format. Use e.g. 5m, 10s. Max 1h.");
                return;
            }
        }

        List<ReplayFrame> frames = session.getRecordedData(durationMs);
        if (frames.isEmpty()) {
            mod.sendMessage("§cNo data found for the requested duration.");
            return;
        }

        mod.sendMessage("§aLoading §e" + frames.size() + "§a frames from active session for §e" + target.getName());
        
        plugin.getReplayPlayerManager().enterReplay(mod);
        PlaybackManager playbackManager = new PlaybackManager(plugin);
        
        // Ensure we use the correct world for start position
        org.bukkit.World world = Bukkit.getWorld(session.getWorldName());
        if (world == null) world = mod.getWorld();
        
        Location startLoc = new Location(world, session.getStartX(), session.getStartY(), session.getStartZ(), session.getStartYaw(), session.getStartPitch());
        
        playbackManager.startPlayback(mod, frames, session.getEntityId(), session.getPlayerUUID(), session.getPlayerName(), startLoc);
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
            try {
                value = Double.parseDouble(input);
                return (long) (value * 60000);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }
    }
}
