package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.SpectateManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpectateCommand extends LifeCommand {

    public SpectateCommand() {
        super("spectate", "lifemod.spectate", true);
        setDescription("Allows you to spectate other players or manage spectator mode.");
        setUsage("/spectate [player|leave|fp|random|back|list]");
    }

    @Override
    public void execute(CommandContext context) {
        SpectateManager spectateManager = context.getPlugin().getSpectateManager();
        if (context.getArgs().length == 0) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.spectate.usage"));
            return;
        }

        String arg = context.getArgs()[0].toLowerCase();

        switch (arg) {
            case "leave":
                spectateManager.leaveSpectate(context.getPlayer());
                break;
            case "fp":
                spectateManager.startFreecam(context.getPlayer());
                break;
            case "random":
                spectateManager.spectateRandom(context.getPlayer());
                break;
            case "back":
                spectateManager.spectateBack(context.getPlayer());
                break;
            case "list":
                spectateManager.sendPlayerList(context.getPlayer());
                break;
            default:
                Player target = Bukkit.getPlayer(arg);
                if (target == null || !target.isOnline()) {
                    context.getSender().sendMessage(context.getLang().getMessage("commands.spectate.player-not-found", "%target%", arg));
                    return;
                }
                spectateManager.startSpectate(context.getPlayer(), target);
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            List<String> suggestions = Arrays.asList("leave", "fp", "random", "back", "list");
            List<String> filtered = TabCompleterUtils.filter(suggestions, context.getArgs()[0]);
            filtered.addAll(TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]));
            return filtered;
        }
        return super.onTabComplete(context);
    }
}
