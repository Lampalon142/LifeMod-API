package fr.lampalon.lifemod.platform.bukkit.commands.impl.utility;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.utils.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StafflistCommand extends LifeCommand {

    public StafflistCommand() {
        super("stafflist", "lifemod.stafflist", true);
        setDescription("View currently online staff members.");
        setUsage("/stafflist");
    }

    @Override
    public void execute(CommandContext context) {
        StringBuilder modList = new StringBuilder(context.getLang().getMessage("commands.stafflist.online"));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("lifemod.stafflist") && !context.getPlugin().getVanishService().isVanished(onlinePlayer.getUniqueId())) {
                modList.append(onlinePlayer.getName()).append(", ");
            }
        }

        if (modList.length() > context.getLang().getMessage("commands.stafflist.online").length()) {
            modList.delete(modList.length() - 2, modList.length());
        } else {
            modList.append(context.getLang().getMessage("commands.stafflist.none"));
        }

        context.getSender().sendMessage(modList.toString());

        if (context.getConfig().getBoolean("modules.discord.enabled", false)) {
            WebhookUtil.sendAlert(context);
        }
    }
}
