package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class BaseSanctionCommand extends LifeCommand {

    protected final SanctionType type;

    public BaseSanctionCommand(String name, String permission, SanctionType type, String description, String usage) {
        super(name, permission, false);
        this.type = type;
        setDescription(description);
        setUsage(usage);
    }

    protected boolean supportsDuration() {
        return true;
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.getArgs();
        if (args.length < 1) {
            String key = supportsDuration() ? "sanctions.cmd.usage" : "sanctions.cmd.usage-no-time";
            context.getSender().sendMessage(context.getLang().getMessage(key, "%cmd%", getName()));
            return;
        }

        String targetName = args[0];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = offlineTarget.getUniqueId();

        boolean silent = false;
        long duration = 0;
        List<String> reasonParts = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (supportsDuration() && arg.matches("\\d+[smhdw]+") && i >= args.length - 2) {
                duration = TimeUtil.parseTime(arg);
            } else {
                reasonParts.add(arg);
            }
        }

        String reason = String.join(" ", reasonParts);
        if (reason.isEmpty()) {
            if (type == SanctionType.NOTE) {
                context.getSender().sendMessage(context.getLang().getMessage("sanctions.cmd.usage-no-time", "%cmd%", getName()));
                return;
            }
            reason = context.getLang().getMessage("sanctions.cmd.default-reason");
        }

        String category = "Other";
        String reasonLower = reason.toLowerCase();
        ConfigurationSection categoriesSec = context.getPlugin().getConfigConfig().getConfigurationSection("auto-punish.categories");
        if (categoriesSec != null) {
            for (String cat : categoriesSec.getKeys(false)) {
                List<String> keywords = context.getLang().getStringList("report.detail.categories." + cat);
                for (String kw : keywords) {
                    if (reasonLower.contains(kw.toLowerCase())) {
                        category = cat;
                        break;
                    }
                }
                if (category.equals("Other") && reasonLower.equals(cat.toLowerCase())) {
                    category = cat;
                }
            }
        }

        String serverName = context.getPlugin().getConfigConfig().getString("server.name", "Survival");

        Sanction sanction = new Sanction(
                targetUuid,
                offlineTarget.getName(),
                context.getSenderUniqueId(),
                context.getSender().getName(),
                serverName,
                category,
                type,
                reason,
                duration,
                silent
        );

        ServiceRegistry.get(ISanctionService.class).applySanction(sanction).thenAccept(s -> {
            onSanctionApplied(context, offlineTarget, s);
        });
    }

    protected abstract void onSanctionApplied(CommandContext context, OfflinePlayer target, Sanction sanction);

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        if (supportsDuration() && context.getArgs().length > 1) {
            String lastArg = context.getArgs()[context.getArgs().length - 1];
            if (!lastArg.equalsIgnoreCase("-s")) {
                List<String> suggestions = new ArrayList<>();
                if (!context.getArgs()[context.getArgs().length - 2].matches("\\d+[smhdw]+")) {
                     suggestions.addAll(Arrays.asList("1h", "1d", "7d", "30d", "permanent"));
                }
                suggestions.add("-s");
                return TabCompleterUtils.filter(suggestions, lastArg);
            }
        }
        return super.onTabComplete(context);
    }
}
