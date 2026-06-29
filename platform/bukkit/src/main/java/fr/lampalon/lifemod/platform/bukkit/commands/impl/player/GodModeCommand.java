package fr.lampalon.lifemod.platform.bukkit.commands.impl.player;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class GodModeCommand extends LifeCommand {

    public GodModeCommand() {
        super("god", "lifemod.god", false);
        setDescription("Toggles invulnerability (god mode) for yourself or another player.");
        setUsage("/god [player]");
    }

    @Override
    public void execute(CommandContext context) {
        Player targetPlayer;

        if (context.getArgs().length > 0) {
            targetPlayer = Bukkit.getPlayer(context.getArgs()[0]);
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

        if (targetPlayer.isInvulnerable()) {
            targetPlayer.setInvulnerable(false);
            targetPlayer.sendMessage(context.getLang().getMessage("commands.utility.god.deactivate.own"));
            if (!targetPlayer.equals(context.getPlayer())) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.utility.god.deactivate.other", "%player%", targetPlayer.getName()));
            }
            context.getDebug().log("god", "God mode disabled for " + targetPlayer.getName() + " by " + context.getSender().getName());
        } else {
            targetPlayer.setInvulnerable(true);
            targetPlayer.sendMessage(context.getLang().getMessage("commands.utility.god.activate.own"));
            if (!targetPlayer.equals(context.getPlayer())) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.utility.god.activate.other", "%player%", targetPlayer.getName()));
            }
            context.getDebug().log("god", "God mode enabled for " + targetPlayer.getName() + " by " + context.getSender().getName());
        }

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context, "generic", Map.of("%target%", targetPlayer.getName(), "%status%", targetPlayer.isInvulnerable() ? "activé" : "désactivé"));
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
