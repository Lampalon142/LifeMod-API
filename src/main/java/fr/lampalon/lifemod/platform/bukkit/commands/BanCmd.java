package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class BanCmd extends BaseSanctionCmd {
    public BanCmd() {
        super("ban", "lifemod.ban", SanctionType.BAN);
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        sender.sendMessage("&aLe joueur &e" + target.getName() + " &aa été banni pour: &f" + sanction.getReason());
        if (target.isOnline()) {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("LifeMod"), () -> {
                target.getPlayer().kickPlayer(MessageUtil.formatMessage("&cVous avez été banni !\n\nRaison: &f" + sanction.getReason()));
            });
        }
    }
}
