package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.nms.api.NMSProvider;
import fr.lampalon.lifemod.platform.bukkit.BukkitPlatform;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import org.bukkit.OfflinePlayer;

public class WarnCommand extends BaseSanctionCommand {
    public WarnCommand() {
        super("warn", "lifemod.warn", SanctionType.WARN, "Warns a player.", "/warn <player> <reason> [-s]");
    }

    @Override
    protected void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction) {
        String success = context.getLang().getMessage("sanctions.warn.success",
                "%player%", target.getName(),
                "%reason%", sanction.getReason());
        context.getSender().sendMessage(success);

        if (target.isOnline()) {
            String received = context.getLang().getMessage("sanctions.warn.received",
                    "%reason%", sanction.getReason());
            target.getPlayer().sendMessage(received);

            ILifePlatform platform = ServiceRegistry.get(ILifePlatform.class);
            if (platform instanceof BukkitPlatform) {
                NMSProvider nms = ((BukkitPlatform) platform).getNmsProvider();
                if (nms != null) {
                    nms.sendTitle(target.getPlayer(), context.getLang().getMessage("sanctions.warn.title"),
                            context.getLang().getMessage("sanctions.warn.subtitle", "%reason%", sanction.getReason()), 10, 40, 10);
                    nms.sendActionBar(target.getPlayer(), received);
                }
            }
        }
    }
}
