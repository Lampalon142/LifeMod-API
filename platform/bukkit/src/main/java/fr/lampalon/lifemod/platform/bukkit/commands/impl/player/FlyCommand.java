package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.common.analytics.IPostHogService;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlyCommand extends LifeCommand {

    public FlyCommand() {
        super("fly", "lifemod.fly", false);
        setDescription("Allows you to fly or toggle flight for another player.");
        setUsage("/fly [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player target;
        boolean isSelf = false;
        
        if (context.getArgs().length == 0) {
            if (!context.isPlayer()) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }
            target = context.getPlayer();
            isSelf = true;
        } else {
            if (!context.getSender().hasPermission("lifemod.fly.others")) {
                context.getSender().sendMessage(context.getLang().getMessage("system.no-permission"));
                return;
            }
            target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }
        }

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        if (newState) target.setFlying(true);

        if (isSelf) {
            String msgKey = newState ? "commands.fly.enabled-self" : "commands.fly.disabled-self";
            context.getSender().sendMessage(context.getLang().getMessage(msgKey));
        } else {
            String msgKeySender = newState ? "commands.fly.enabled" : "commands.fly.disabled";
            String msgKeyTarget = newState ? "commands.fly.enabled-by" : "commands.fly.disabled-by";
            
            context.getSender().sendMessage(context.getLang().getMessage(msgKeySender, "%player%", target.getName()));
            target.sendMessage(context.getLang().getMessage(msgKeyTarget, "%player%", context.getSender().getName()));
        }

        IPostHogService ph = ServiceRegistry.get(IPostHogService.class);
        if (ph != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("action", newState ? "enable" : "disable");
            ph.capture("lifemod_fly", props);
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1 && context.getSender().hasPermission("lifemod.fly.others")) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
