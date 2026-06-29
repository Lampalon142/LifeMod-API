package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;


import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.IVanishService;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class VanishCommand extends LifeCommand {
    private final LifeMod plugin;
    private final IVanishService vanishService;

    public VanishCommand(LifeMod plugin) {
        super("vanish", "lifemod.vanish", true, "v");
        this.plugin = plugin;
        this.vanishService = plugin.getVanishService();
        setDescription("Toggles player visibility (vanish).");
        setUsage("/vanish [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.getPlayer();

        if (context.getArgs().length == 0) {
            boolean newState = !vanishService.isVanished(player.getUniqueId());
            vanishService.setVanished(player, newState, false);
            String msgKey = newState ? "vanish.activate" : "vanish.deactivate";
            context.getSender().sendMessage(context.getLang().getMessage(msgKey));
            context.getDebug().log("vanish", player.getName() + " toggled vanish to " + newState);
        } else if (context.getArgs().length == 1) {
            Player target = Bukkit.getPlayer(context.getArgs()[0]);
            if (target != null) {
                boolean newState = !vanishService.isVanished(target.getUniqueId());
                vanishService.setVanished(target, newState, false);
                
                String targetMsgKey = newState ? "vanish.activate" : "vanish.deactivate";
                String senderMsgKey = newState ? "mod.items.vanish-on" : "mod.items.vanish-off";
                
                target.sendMessage(context.getLang().getMessage(targetMsgKey));
                context.getSender().sendMessage(context.getLang().getMessage(senderMsgKey));
                context.getDebug().log("vanish", player.getName() + " toggled vanish for " + target.getName() + " to " + newState);
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            }
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context);
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
