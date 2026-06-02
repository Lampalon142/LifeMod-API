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
import java.util.UUID;

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
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("sanctions.cmd.revoke-usage", "%cmd%", getName()));
            return;
        }

        String targetName = context.getArgs()[0];
        UUID targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        
        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();
        for (int i = 1; i < context.getArgs().length; i++) {
            if (context.getArgs()[i].equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                reasonParts.add(context.getArgs()[i]);
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
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        if (context.getArgs().length > 1) {
            String lastArg = context.getArgs()[context.getArgs().length - 1];
            return TabCompleterUtils.filter(List.of("-s"), lastArg);
        }
        return super.onTabComplete(context);
    }
}
