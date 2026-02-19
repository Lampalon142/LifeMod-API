package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRevokeCommand extends LifeCommand {
    private final SanctionType type;

    public BaseRevokeCommand(String name, String permission, SanctionType type, String description, String usage) {
        super(name, permission, false);
        this.type = type;
        setDescription(description);
        setUsage(usage);
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.getArgs();
        
        if (args.length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("sanctions.cmd.revoke-usage", "%cmd%", getName()));
            return;
        }

        String targetName = args[0];
        java.util.UUID targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        
        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                reasonParts.add(args[i]);
            }
        }

        String reason = reasonParts.isEmpty() ? context.getLang().getMessage("sanctions.cmd.default-reason") : String.join(" ", reasonParts);

        boolean finalSilent = silent;
        ServiceRegistry.get(ISanctionService.class).revokeSanction(
                targetUuid, type, context.getSenderUniqueId(), context.getSender().getName(), reason, silent
        ).thenAccept(success -> {
            if (success) {
                String msgKey = finalSilent ? "sanctions.cmd.revoke-silent" : "sanctions.cmd.revoke-success";
                String msg = context.getLang().getMessage(msgKey, "%type%", type.name(), "%player%", targetName);
                context.getSender().sendMessage(msg);
            } else {
                context.getSender().sendMessage(context.getLang().getMessage("sanctions.cmd.revoke-not-found", "%type%", type.name()));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(args[0]);
        }
        if (args.length > 1) {
            // Suggest -s for silent, but only if not already present
            if (!args[args.length - 1].equalsIgnoreCase("-s") && !args[args.length - 1].isEmpty()) {
                 return filter(List.of("-s"), context);
            }
        }
        return super.onTabComplete(context);
    }
}
