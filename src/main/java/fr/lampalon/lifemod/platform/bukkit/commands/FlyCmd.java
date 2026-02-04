package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class FlyCmd extends LifeCommand {
    private final LifeMod plugin;

    public FlyCmd(LifeMod plugin) {
        super("fly", "lifemod.fly", false);
        this.plugin = plugin;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        Player target;
        boolean isSelf = false;

        if (args.length == 0) {
            if (!sender.isPlayer()) {
                sender.sendMessage("general.onlyplayer");
                return;
            }
            target = Bukkit.getPlayer(sender.getUniqueId());
            isSelf = true;
        } else {
            if (!sender.hasPermission("lifemod.fly.others")) {
                sender.sendMessage("general.nopermission");
                return;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("general.offlineplayer");
                return;
            }
        }

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        if (newState) target.setFlying(true);

        if (isSelf) {
            String msgKey = newState ? "fly.enabled-self" : "fly.disabled-self";
            sender.sendMessage(plugin.getLangConfig().getString(msgKey));
        } else {
            String msgKeySender = newState ? "fly.enabled" : "fly.disabled";
            String msgKeyTarget = newState ? "fly.enabled-self" : "fly.disabled-self";
            
            sender.sendMessage(plugin.getLangConfig().getString(msgKeySender).replace("%player%", target.getName()));
            target.sendMessage(plugin.getLangConfig().getString(msgKeyTarget).replace("%player%", sender.getName()));
        }
        
        // Note: La logique Discord devrait idéalement être dans un Service (DiscordService)
        // déclenché par un événement ou un appel direct au service.
    }

    @Override
    public List<String> onTabComplete(ICommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("lifemod.fly.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
