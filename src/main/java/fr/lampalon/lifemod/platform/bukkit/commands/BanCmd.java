package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class BanCmd extends BaseSanctionCmd {
    public BanCmd() {
        super("ban", "lifemod.ban", SanctionType.BAN);
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        String success = LifeMod.getInstance().getLangConfig().getString("ban.success", "&aPlayer &e%player% &ahas been banned for: &f%reason%")
                .replace("%player%", target.getName())
                .replace("%reason%", sanction.getReason());
        sender.sendMessage(MessageUtil.formatMessage(success));
        
        if (target.isOnline()) {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("LifeMod"), () -> {
                String kickMsg = LifeMod.getInstance().getLangConfig().getString("ban.kick-message", "&cYou have been banned!\n\nReason: &f%reason%")
                        .replace("%reason%", sanction.getReason())
                        .replace("%issuer%", sanction.getIssuerName())
                        .replace("%time%", fr.lampalon.lifemod.common.utils.TimeUtil.formatTime(sanction.getDuration()));
                target.getPlayer().kickPlayer(MessageUtil.formatMessage(kickMsg));
            });
        }
    }
}
