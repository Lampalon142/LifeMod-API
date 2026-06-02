package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class CaseCommand extends LifeCommand {

    public CaseCommand() {
        super("case", "lifemod.case", false);
        setDescription("View a player's moderation case.");
        setUsage("/case <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("gui.case.usage"));
            return;
        }

        String targetName = context.getArgs()[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();

        ISanctionService ss = ServiceRegistry.get(ISanctionService.class);

        context.getSender().sendMessage(context.getLang().getMessage("gui.case.header", "%target%", target.getName()));

        // Check active ban
        ss.getActiveSanction(uuid, target.getName(), SanctionType.BAN).thenAccept(ban -> {
            if (ban != null && !ban.isExpired()) {
                context.getSender().sendMessage(context.getLang().getMessage("gui.case.ban-active", "%reason%", ban.getReason(), "%time%", TimeUtil.formatTime(ban.getExpirationTime() - System.currentTimeMillis())));
            } else if (ban != null && ban.isExpired()) {
                context.getSender().sendMessage(context.getLang().getMessage("gui.case.ban-expired", "%reason%", ban.getReason()));
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("gui.case.ban-none"));
            }
        });

        // Check active mute
        ss.getActiveSanction(uuid, target.getName(), SanctionType.MUTE).thenAccept(mute -> {
            if (mute != null && !mute.isExpired()) {
                context.getSender().sendMessage(context.getLang().getMessage("gui.case.mute-active", "%reason%", mute.getReason(), "%time%", TimeUtil.formatTime(mute.getExpirationTime() - System.currentTimeMillis())));
            } else if (mute != null && mute.isExpired()) {
                context.getSender().sendMessage(context.getLang().getMessage("gui.case.mute-expired", "%reason%", mute.getReason()));
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("gui.case.mute-none"));
            }
        });

        // History summary
        ss.getHistory(uuid).thenAccept(history -> {
            long warns = history.stream().filter(s -> s.getType() == SanctionType.WARN && s.isActive() && !s.isExpired()).count();
            context.getSender().sendMessage(context.getLang().getMessage("gui.case.warns", "%count%", String.valueOf(warns)));
            context.getSender().sendMessage(context.getLang().getMessage("gui.case.total", "%count%", String.valueOf(history.size())));
            context.getSender().sendMessage(context.getLang().getMessage("gui.case.footer", "%target%", targetName));
        });
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
