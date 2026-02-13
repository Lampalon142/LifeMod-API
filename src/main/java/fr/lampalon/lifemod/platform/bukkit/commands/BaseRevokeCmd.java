package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class BaseRevokeCmd extends LifeCommand {

    private final SanctionType type;

    public BaseRevokeCmd(String name, String permission, SanctionType type) {
        super(name, permission, false);
        this.type = type;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        
        if (args.length < 1) {
            sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("sanctions.cmd.revoke-usage").replace("%cmd%", getName())));
            return;
        }

        String targetName = args[0];
        UUID targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        
        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                reasonParts.add(args[i]);
            }
        }

        String reason = reasonParts.isEmpty() ? lang.getMessage("sanctions.cmd.default-reason", "Revoked by an administrator") : String.join(" ", reasonParts);

        boolean finalSilent = silent;
        ServiceRegistry.get(ISanctionService.class).revokeSanction(
                targetUuid, type, sender.getUniqueId(), sender.getName(), reason, silent
        ).thenAccept(success -> {
            if (success) {
                // Le broadcast est géré dans SanctionService
                String msgKey = finalSilent ? "sanctions.cmd.revoke-silent" : "sanctions.cmd.revoke-success";
                String msg = lang.getMessage(msgKey)
                        .replace("%type%", type.name())
                        .replace("%player%", targetName);
                sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(msg));
            } else {
                sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("sanctions.cmd.revoke-not-found").replace("%type%", type.name())));
            }
        });
    }
}

