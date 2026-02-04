package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import org.bukkit.Bukkit;

import java.util.UUID;

public abstract class BaseRevokeCmd extends LifeCommand {

    private final SanctionType type;

    public BaseRevokeCmd(String name, String permission, SanctionType type) {
        super(name, permission, false);
        this.type = type;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("&cUsage: /" + getName() + " [joueur] <raison>");
            return;
        }

        String targetName = args[0];
        UUID targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "Révoqué par un administrateur";

        ServiceRegistry.get(ISanctionService.class).revokeSanction(
                targetUuid, type, sender.getUniqueId(), sender.getName(), reason
        ).thenAccept(success -> {
            if (success) {
                sender.sendMessage("&aLa sanction (" + type.name() + ") de &e" + targetName + " &aa été levée.");
            } else {
                sender.sendMessage("&cCe joueur n'a pas de sanction active de type " + type.name());
            }
        });
    }
}

