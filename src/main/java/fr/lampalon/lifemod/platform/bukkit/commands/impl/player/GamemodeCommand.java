package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GamemodeCommand extends LifeCommand {

    private final boolean useLuckPerms;
    private LuckPerms luckPerms;
    private final LifeMod plugin;

    public GamemodeCommand(LifeMod plugin) {
        super("gamemode", "lifemod.gm", false, "gm");
        this.plugin = plugin;
        setDescription("Changes the game mode of yourself or another player.");
        setUsage("/gamemode <mode> [player]");
        this.useLuckPerms = plugin.getConfigConfig().getBoolean("UseLuckPerms");
        if (this.useLuckPerms) {
            try {
                this.luckPerms = LuckPermsProvider.get();
            } catch (IllegalStateException e) {
                Bukkit.getLogger().warning("[LifeMod] LuckPerms is configured but not detected!");
            }
        }
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1 || context.getArgs().length > 2) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.gamemode.invalid"));
            return;
        }

        Player targetPlayer;
        if (context.getArgs().length == 2) {
            targetPlayer = Bukkit.getPlayer(context.getArgs()[1]);
            if (targetPlayer == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }
        } else {
            if (!context.isPlayer()) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }
            targetPlayer = context.getPlayer();
        }

        GameMode gameMode = parseGameMode(context.getArgs()[0]);
        if (gameMode == null) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.gamemode.invalid"));
            return;
        }

        targetPlayer.setGameMode(gameMode);
        String playerPrefix = getPlayerPrefix(targetPlayer);

        String msgKey = (context.getArgs().length == 2) ? "commands.gamemode.other" : "commands.gamemode.own";
        String message = context.getLang().getMessage(msgKey)
                .replace("%gamemode%", gameMode.name().toLowerCase())
                .replace("%player%", targetPlayer.getName())
                .replace("%luckperms_prefix%", playerPrefix);

        context.getSender().sendMessage(message);
        context.getDebug().log("gm", context.getSender().getName() + " changed gamemode of " + targetPlayer.getName() + " to " + gameMode.name());
    }

    private GameMode parseGameMode(String modeArg) {
        modeArg = modeArg.toLowerCase();
        switch (modeArg) {
            case "0": case "s": case "survival": return GameMode.SURVIVAL;
            case "1": case "c": case "creative": return GameMode.CREATIVE;
            case "2": case "a": case "adventure": return GameMode.ADVENTURE;
            case "3": case "sp": case "spectator": return GameMode.SPECTATOR;
            default: return null;
        }
    }

    private String getPlayerPrefix(Player player) {
        if (this.useLuckPerms && this.luckPerms != null) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                return prefix != null ? prefix : ""; // MessageUtil.parseColors should be handled by the lang service
            }
        }
        return "";
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filter(Arrays.asList("survival", "creative", "adventure", "spectator", "0", "1", "2", "3"), context.getArgs()[0]);
        } else if (context.getArgs().length == 2) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[1]);
        }
        return super.onTabComplete(context);
    }
}
