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
            context.getSender().sendMessage(context.getLang().getMessage("commands.replay.player-only"));
            return;
        }

        Player mod = (Player) context.getSender();
        ReplayPlayerManager rpm = plugin.getReplayPlayerManager();

        if (context.getArgs().length < 1) {
            sendHelp(mod, context);
            return;
        }

        String action = context.getArgs()[0];

        if (action.equalsIgnoreCase("stop") || action.equalsIgnoreCase("exit")) {
            if (!rpm.isInReplay(mod)) {
                mod.sendMessage(context.getLang().getMessage("commands.replay.not-in-replay"));
                return;
            }
            rpm.exitReplay(mod);
            mod.sendMessage(context.getLang().getMessage("commands.replay.stopped"));
            return;
        }

        if (action.equalsIgnoreCase("list")) {
            listReplays(mod, context);
            return;
        }

        if (action.equalsIgnoreCase("load")) {
            if (context.getArgs().length < 2) {
                mod.sendMessage(context.getLang().getMessage("commands.replay.usage-load"));
                return;
            }
            loadAndPlay(mod, context.getArgs()[1], context);
            return;
        }

        playFromActive(mod, action, context.getArgs(), context);
    }

    private void sendHelp(Player mod, CommandContext context) {
        mod.sendMessage(context.getLang().getMessage("commands.replay.help-title"));
        mod.sendMessage(context.getLang().getMessage("commands.replay.help-play"));
        mod.sendMessage(context.getLang().getMessage("commands.replay.help-load"));
        mod.sendMessage(context.getLang().getMessage("commands.replay.help-list"));
        mod.sendMessage(context.getLang().getMessage("commands.replay.help-stop"));
        mod.sendMessage(context.getLang().getMessage("commands.replay.help-duration"));
    }

    private void listReplays(Player mod, CommandContext context) {
        File replayDir = new File("plugins/LifeMod/replays");
        if (!replayDir.exists() || !replayDir.isDirectory()) {
            mod.sendMessage(context.getLang().getMessage("commands.replay.no-replays"));
            return;
        }
        File[] files = replayDir.listFiles((dir, name) -> name.endsWith(".replay"));
        if (files == null || files.length == 0) {
            mod.sendMessage(context.getLang().getMessage("commands.replay.no-replays"));
            return;
        }
        mod.sendMessage(context.getLang().getMessage("commands.replay.list-title"));
        for (File f : files) {
            mod.sendMessage(context.getLang().getMessage("commands.replay.list-entry",
                    "%name%", f.getName().replace(".replay", ""),
                    "%size%", String.valueOf(f.length() / 1024)));
        }
    }

    private void loadAndPlay(Player mod, String sessionName, CommandContext context) {
        File replayFile = new File("plugins/LifeMod/replays", sessionName + ".replay");
        if (!replayFile.exists()) {
            mod.sendMessage(context.getLang().getMessage("commands.replay.file-not-found", "%name%", sessionName));
            return;
        }

        mod.sendMessage(context.getLang().getMessage("commands.replay.reading-file"));

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
                    mod.sendMessage(context.getLang().getMessage("commands.replay.file-empty"));
                    return;
                }
                @SuppressWarnings("unchecked")
                List<ReplayFrame> frames = (List<ReplayFrame>) result[0];
                UUID targetUUID = (UUID) result[1];
                String targetName = (String) result[2];
                int entityId = (int) result[3];
                Location startLoc = (Location) result[4];

                mod.sendMessage(context.getLang().getMessage("commands.replay.loaded-frames", "%count%", String.valueOf(frames.size()), "%player%", targetName));
                plugin.getReplayPlayerManager().enterReplay(mod);
                PlaybackManager playbackManager = new PlaybackManager(plugin);
                playbackManager.startPlayback(mod, frames, entityId, targetUUID, targetName, startLoc);
            });
        });
    }

    private void playFromActive(Player mod, String targetName, String[] args, CommandContext context) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            mod.sendMessage(context.getLang().getMessage("commands.replay.player-not-found", "%name%", targetName));
            return;
        }

        ReplaySession session = plugin.getReplayManager().getSession(target.getUniqueId());
        if (session == null) {
            mod.sendMessage(context.getLang().getMessage("commands.replay.no-active-recording", "%player%", target.getName()));
            return;
        }

        long durationMs = 60000;
        if (args.length >= 2) {
            try {
                durationMs = parseDuration(args[1]);
            } catch (IllegalArgumentException e) {
                mod.sendMessage(context.getLang().getMessage("commands.replay.invalid-duration"));
                return;
            }
        }

        List<ReplayFrame> frames = session.getRecordedData(durationMs);
        if (frames.isEmpty()) {
            mod.sendMessage(context.getLang().getMessage("commands.replay.no-data"));
            return;
        }

        mod.sendMessage(context.getLang().getMessage("commands.replay.loading-frames", "%count%", String.valueOf(frames.size()), "%player%", target.getName()));
        
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
