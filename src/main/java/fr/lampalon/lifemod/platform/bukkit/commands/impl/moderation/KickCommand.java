package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class KickCommand extends BaseSanctionCommand {
    public KickCommand() {
        super("kick", "lifemod.kick", SanctionType.KICK, "Kicks a player from the server.", "/kick <player> <reason> [-s]");
    }

    @Override
    protected boolean supportsDuration() {
        return false;
    }

    @Override
    protected void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction) {
        String successMsg = context.getLang().getMessage("sanctions.kick.success",
                "%player%", target.getName(),
                "%reason%", sanction.getReason());
        context.getSender().sendMessage(successMsg);
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                String kickMsg = context.getLang().getMessage("sanctions.kick.message",
                        "%reason%", sanction.getReason(),
                        "%issuer%", sanction.getIssuerName());

                ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
                if (platform instanceof BukkitPlatform) {
                    NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
                    if (nms != null) {
                        nms.kickPlayer(onlineTarget, kickMsg);
                    }
                }
            });
        }
    }
}
