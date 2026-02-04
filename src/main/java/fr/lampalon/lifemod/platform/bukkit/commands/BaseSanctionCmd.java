package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.UUID;

public abstract class BaseSanctionCmd extends LifeCommand {

    protected final SanctionType type;

    public BaseSanctionCmd(String name, String permission, SanctionType type) {
        super(name, permission, false);
        this.type = type;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length < 2 && type != SanctionType.NOTE) {
            sender.sendMessage("&cUsage: /" + getName() + " [joueur] [raison] <temps> <-s>");
            return;
        }

        String targetName = args[0];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = offlineTarget.getUniqueId();

        boolean silent = false;
        long duration = 0;
        int reasonEndIndex = args.length;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-s")) {
                silent = true;
                reasonEndIndex = Math.min(reasonEndIndex, i);
            }
            if (i == args.length - 1 && args[i].matches("\\d+[smhdw]")) {
                duration = parseTime(args[i]);
                reasonEndIndex = Math.min(reasonEndIndex, i);
            }
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, reasonEndIndex));
        if (reason.isEmpty() && type != SanctionType.NOTE) {
            sender.sendMessage("&cVous devez spécifier une raison.");
            return;
        }

        Sanction sanction = new Sanction(
                targetUuid,
                sender.getUniqueId(),
                sender.getName(),
                type,
                reason,
                duration,
                silent
        );

        ServiceRegistry.get(ISanctionService.class).applySanction(sanction).thenAccept(s -> {
            onSanctionApplied(sender, offlineTarget, s);
        });
    }

    protected abstract void onSanctionApplied(ICommandSender sender, OfflinePlayer target, Sanction sanction);

    private long parseTime(String time) {
        try {
            long val = Long.parseLong(time.substring(0, time.length() - 1));
            char unit = time.charAt(time.length() - 1);
            switch (unit) {
                case 's': return val * 1000;
                case 'm': return val * 60 * 1000;
                case 'h': return val * 3600 * 1000;
                case 'd': return val * 86400 * 1000;
                case 'w': return val * 7 * 86400 * 1000;
                default: return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}

