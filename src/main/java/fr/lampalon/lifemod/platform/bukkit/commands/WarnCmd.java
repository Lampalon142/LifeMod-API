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
            fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
            String received = lang.getMessage("sanctions.warn.received", "&c&lWARNING! &7Reason: &f%reason%")
                    .replace("%reason%", sanction.getReason());
            target.getPlayer().sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(received));
            
            // Ultra complete visual warning
            fr.lampalon.lifemod.integration.nms.PacketController pc = fr.lampalon.lifemod.platform.bukkit.LifeMod.getInstance().getPacketController();
            String title = lang.getMessage("sanctions.warn.title", "&c&lWARNING");
            String subtitle = lang.getMessage("sanctions.warn.subtitle", "&7%reason%").replace("%reason%", sanction.getReason());
            
            pc.sendTitle(target.getPlayer(), title, subtitle, 10, 40, 10);
            pc.sendActionBar(target.getPlayer(), received);
        }
    }
}

