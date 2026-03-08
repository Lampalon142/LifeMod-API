package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.SpectateManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpectateCommand extends LifeCommand {
    private final SpectateManager spectateManager;

    public SpectateCommand(LifeMod plugin) {
        super("spectate", "lifemod.spectate", true);
        this.spectateManager = plugin.getSpectateManager();
        setDescription("Allows you to spectate other players or manage spectator mode.");
        setUsage("/spectate [player|leave|fp|random|back|list]");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();

        if (args.length == 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.spectate.usage"));
            return;
        }

        String arg = args[0].toLowerCase();

        switch (arg) {
            case "leave":
                spectateManager.leaveSpectate(player);
                break;
            case "fp":
                spectateManager.startFreecam(player);
                break;
            case "random":
                spectateManager.spectateRandom(player);
                break;
            case "back":
                spectateManager.spectateBack(player);
                break;
            case "list":
                spectateManager.sendPlayerList(player);
                break;
            default:
                Player target = Bukkit.getPlayer(arg);
                if (target == null || !target.isOnline()) {
                    context.getSender().sendMessage(context.getLang().getMessage("commands.spectate.player-not-found", "%target%", arg));
                    return;
                }
                spectateManager.startSpectate(player, target);
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("leave", "fp", "random", "back", "list"));
            suggestions = filter(suggestions, context);
            suggestions.addAll(TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]));
            return suggestions;
        }
        return super.onTabComplete(context);
    }
}
