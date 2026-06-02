package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class BanCommand extends BaseSanctionCommand {
    public BanCommand() {
        super("ban", "lifemod.ban", SanctionType.BAN, "Bans a player from the server.", "/ban <player> <reason> [duration] [-s]");
    }

    @Override
    protected void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction) {
        String success = context.getLang().getMessage("sanctions.ban.success",
                "%player%", target.getName(),
                "%reason%", sanction.getReason());
        context.getSender().sendMessage(success);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                String kickMsg = context.getLang().getMessage("sanctions.ban.received",
                        "%reason%", sanction.getReason(),
                        "%issuer%", sanction.getIssuerName(),
                        "%time%", fr.lampalon.lifemod.common.utils.TimeUtil.formatTime(sanction.getDuration()));

                ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
                platform.kickPlayer(target.getUniqueId(), kickMsg);
            });
        }
    }
}
