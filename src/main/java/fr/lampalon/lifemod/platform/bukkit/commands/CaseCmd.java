package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class CaseCmd extends LifeCommand {

    public CaseCmd() {
        super("case", "lifemod.case", false);
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.usage")));
            return;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();

        ISanctionService ss = ServiceRegistry.get(ISanctionService.class);

        sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.header", "%target%", target.getName())));

        // Check active ban
        ss.getActiveSanction(uuid, target.getName(), SanctionType.BAN).thenAccept(ban -> {
            if (ban != null && !ban.isExpired()) {
                sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.ban-active", "%reason%", ban.getReason(), "%time%", TimeUtil.formatTime(ban.getExpirationTime() - System.currentTimeMillis()))));
            } else if (ban != null && ban.isExpired()) {
                sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.ban-expired", "%reason%", ban.getReason())));
            } else {
                sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.ban-none")));
            }
        });

        // Check active mute
        ss.getActiveSanction(uuid, target.getName(), SanctionType.MUTE).thenAccept(mute -> {
            if (mute != null && !mute.isExpired()) {
                sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.mute-active", "%reason%", mute.getReason(), "%time%", TimeUtil.formatTime(mute.getExpirationTime() - System.currentTimeMillis()))));
            } else if (mute != null && mute.isExpired()) {
                sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.mute-expired", "%reason%", mute.getReason())));
            } else {
                sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.mute-none")));
            }
        });

        // History summary
        ss.getHistory(uuid).thenAccept(history -> {
            long warns = history.stream().filter(s -> s.getType() == SanctionType.WARN && s.isActive() && !s.isExpired()).count();
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.warns", "%count%", String.valueOf(warns))));
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.total", "%count%", String.valueOf(history.size()))));
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("gui.case.footer", "%target%", targetName)));
        });
    }
}
