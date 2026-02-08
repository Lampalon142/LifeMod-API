package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class KickCmd extends BaseSanctionCmd {
    public KickCmd() {
        super("kick", "lifemod.kick", SanctionType.KICK);
    }

    @Override
    protected boolean supportsDuration() {
        return false;
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        String successMsg = LifeMod.getInstance().getLangConfig().getString("kick.success", "&aPlayer &e%target% &ahas been kicked for: &f%reason%")
                .replace("%target%", target.getName())
                .replace("%reason%", sanction.getReason());
        sender.sendMessage(MessageUtil.formatMessage(successMsg));
        if (target.isOnline()) {
            Bukkit.getScheduler().runTask(LifeMod.getInstance(), () -> {
                String kickMsg = LifeMod.getInstance().getLangConfig().getString("kick.kick-message", "&cYou have been kicked! Reason: &f%reason%")
                        .replace("%reason%", sanction.getReason())
                        .replace("%issuer%", sanction.getIssuerName());
                target.getPlayer().kickPlayer(MessageUtil.formatMessage(kickMsg));
            });
        }
    }
}
