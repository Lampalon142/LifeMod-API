package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.managers.FreezeManager;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class FreezeCommand extends LifeCommand {
    private final FreezeManager freezeManager;
    private final LifeMod plugin; // Keep plugin reference for managers

    public FreezeCommand(LifeMod plugin) { // Constructor now takes LifeMod
        super("freeze", "lifemod.freeze", true);
        this.plugin = plugin;
        this.freezeManager = plugin.getFreezeManager();
        setDescription("Freezes or unfreezes a player.");
        setUsage("/freeze <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(context.getArgs()[0]);
        if (target == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        if (target.equals(context.getPlayer())) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.yourself"));
            return;
        }

        if (freezeManager.isPlayerFrozen(target.getUniqueId())) {
            freezeManager.unfreezePlayer(context.getPlayer(), target);
            target.sendMessage(context.getLang().getMessage("commands.freeze.messages.unfreeze.target", "%player%", context.getSender().getName()));
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.messages.unfreeze.mod", "%target%", target.getName()));
            context.getDebug().log("freeze", context.getSender().getName() + " unfroze " + target.getName());
        } else {
            freezeManager.freezePlayer(context.getPlayer(), target);
            context.getLang().getStringList("commands.freeze.messages.freeze.onfreeze")
                    .forEach(target::sendMessage);
            context.getSender().sendMessage(context.getLang().getMessage("commands.freeze.messages.freeze.mod", "%target%", target.getName()));
            context.getDebug().log("freeze", context.getSender().getName() + " froze " + target.getName());
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
