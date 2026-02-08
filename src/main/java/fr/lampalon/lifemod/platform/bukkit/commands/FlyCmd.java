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
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);

        if (args.length == 0) {
            if (!sender.isPlayer()) {
                sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("system.player-only")));
                return;
            }
            target = Bukkit.getPlayer(sender.getUniqueId());
            isSelf = true;
        } else {
            if (!sender.hasPermission("lifemod.fly.others")) {
                sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("system.no-permission")));
                return;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("system.player-not-found")));
                return;
            }
        }

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        if (newState) target.setFlying(true);

        if (isSelf) {
            String msgKey = newState ? "commands.fly.enabled-self" : "commands.fly.disabled-self";
            sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage(msgKey)));
        } else {
            String msgKeySender = newState ? "commands.fly.enabled" : "commands.fly.disabled";
            String msgKeyTarget = newState ? "commands.fly.enabled-by" : "commands.fly.disabled-by";
            
            sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage(msgKeySender, "%player%", target.getName())));
            target.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage(msgKeyTarget, "%player%", sender.getName())));
        }
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
