package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class MuteCommand extends BaseSanctionCommand {
    public MuteCommand() {
        super("mute", "lifemod.mute", SanctionType.MUTE, "Mutes a player in the chat.", "/mute <player> <reason> [duration] [-s]");
    }

    @Override
    protected void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction) {
        String success = context.getLang().getMessage("sanctions.mute.success",
                "%player%", target.getName(),
                "%reason%", sanction.getReason());
        context.getSender().sendMessage(success);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            String received = context.getLang().getMessage("sanctions.mute.received",
                    "%reason%", sanction.getReason());
            onlineTarget.sendMessage(received);

            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            if (platform instanceof BukkitPlatform) {
                NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
                if (nms != null) {
                    nms.sendTitle(onlineTarget, context.getLang().getMessage("sanctions.mute.title"),
                            context.getLang().getMessage("sanctions.mute.subtitle", "%reason%", sanction.getReason()), 10, 40, 10);
                    nms.sendActionBar(onlineTarget, received);
                }
            }
        }
    }
}
