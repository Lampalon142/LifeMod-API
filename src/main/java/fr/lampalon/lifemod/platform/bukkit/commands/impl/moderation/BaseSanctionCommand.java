package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseSanctionCommand extends LifeCommand {
    protected final SanctionType type;
    protected final LifeMod plugin;

    public BaseSanctionCommand(String name, String permission, SanctionType type, String description, String usage, LifeMod plugin) {
        super(name, permission, false);
        this.type = type;
        this.plugin = plugin;
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
        java.util.UUID targetUuid = offlineTarget.getUniqueId();

        boolean silent = false;
        long duration = 0;
        List<String> reasonParts = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (supportsDuration() && arg.matches("\\d+[smhdw]+") && i >= args.length - 2) {
                // On considère que le temps est à la fin (LiteBans style)
                duration = TimeUtil.parseTime(arg);
            } else {
                reasonParts.add(arg);
            }
        }

        String reason = String.join(" ", reasonParts);
        if (reason.isEmpty() && type != SanctionType.NOTE) {
            reason = context.getLang().getMessage("sanctions.cmd.default-reason");
        }

        String category = "Other";
        ConfigurationSection categoriesSec = plugin.getConfigConfig().getConfigurationSection("auto-punish.categories");
        if (categoriesSec != null) {
            for (String cat : categoriesSec.getKeys(false)) {
                List<String> keywords = plugin.getLangConfig().getStringList("report.detail.categories." + cat);
                for (String kw : keywords) {
                    if (reason.toLowerCase().contains(kw.toLowerCase())) {
                        category = cat;
                        break;
                    }
                }
            }
        }

        String serverName = plugin.getConfigConfig().getString("server.name", "Survival");

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
        String[] args = context.getArgs();
        if (args.length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(args[0]);
        }
        if (supportsDuration() && args.length > 1) {
            String lastArg = args[args.length - 1];
            if (!lastArg.equalsIgnoreCase("-s")) {
                List<String> suggestions = new ArrayList<>();
                if (args.length >= 2 && !args[args.length - 2].matches("\\d+[smhdw]+")) { // Only suggest time if not already given
                     suggestions.addAll(Arrays.asList("1h", "1d", "7d", "30d", "permanent"));
                }
                if (!lastArg.equalsIgnoreCase("-s")) { // Only suggest -s if not already given
                    suggestions.add("-s");
                }
                return filter(suggestions, context);
            }
        }
        return super.onTabComplete(context);
    }
}
