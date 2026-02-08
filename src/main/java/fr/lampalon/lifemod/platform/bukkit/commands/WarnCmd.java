package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import org.bukkit.OfflinePlayer;

public class WarnCmd extends BaseSanctionCmd {
    public WarnCmd() {
        super("warn", "lifemod.warn", SanctionType.WARN);
    }

    @Override
    protected void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction) {
        String success = fr.lampalon.lifemod.platform.bukkit.LifeMod.getInstance().getLangConfig().getString("sanctions.warn.success", "&aWarning sent to &e%player% &afor: &f%reason%")
                .replace("%player%", target.getName())
                .replace("%reason%", sanction.getReason());
        sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(success));
        
        if (target.isOnline()) {
            String received = fr.lampalon.lifemod.platform.bukkit.LifeMod.getInstance().getLangConfig().getString("sanctions.warn.received", "&c&lWARNING! &7Reason: &f%reason%")
                    .replace("%reason%", sanction.getReason());
            target.getPlayer().sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(received));
        }
    }
}

