package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import org.bukkit.OfflinePlayer;

public class MuteCmd extends BaseSanctionCmd {
    public MuteCmd() {
        super("mute", "lifemod.mute", SanctionType.MUTE);
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        String success = fr.lampalon.lifemod.platform.bukkit.LifeMod.getInstance().getLangConfig().getString("sanctions.mute.success", "&aPlayer &e%player% &ahas been muted for: &f%reason%")
                .replace("%player%", target.getName())
                .replace("%reason%", sanction.getReason());
        sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(success));
        
        if (target.isOnline()) {
            String received = fr.lampalon.lifemod.platform.bukkit.LifeMod.getInstance().getLangConfig().getString("sanctions.mute.received", "&cYou have been muted! Reason: &f%reason%")
                    .replace("%reason%", sanction.getReason());
            target.getPlayer().sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(received));
        }
    }
}

