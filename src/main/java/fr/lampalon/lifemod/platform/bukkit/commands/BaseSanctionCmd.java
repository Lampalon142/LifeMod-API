package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class BaseSanctionCmd extends LifeCommand {

    protected final SanctionType type;

    public BaseSanctionCmd(String name, String permission, SanctionType type) {
        super(name, permission, false);
        this.type = type;
    }

    protected boolean supportsDuration() {
        return true;
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            String key = supportsDuration() ? "sanctions.cmd.usage" : "sanctions.cmd.usage-no-time";
            String usage = LifeMod.getInstance().getLangConfig().getString(key)
                    .replace("%cmd%", getName());
            sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(usage));
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
                // On considère que le temps est à la fin (LiteBans style)
                duration = TimeUtil.parseTime(arg);
            } else {
                reasonParts.add(arg);
            }
        }

        String reason = String.join(" ", reasonParts);
        if (reason.isEmpty() && type != SanctionType.NOTE) {
            reason = LifeMod.getInstance().getLangConfig().getString("sanctions.cmd.default-reason");
        }

        String category = "Other";
        ConfigurationSection categoriesSec = LifeMod.getInstance().getConfigConfig().getConfigurationSection("auto-punish.categories");
        if (categoriesSec != null) {
            for (String cat : categoriesSec.getKeys(false)) {
                List<String> keywords = LifeMod.getInstance().getLangConfig().getStringList("report.detail.categories." + cat);
                for (String kw : keywords) {
                    if (reason.toLowerCase().contains(kw.toLowerCase())) {
                        category = cat;
                        break;
                    }
                }
            }
        }

        String serverName = LifeMod.getInstance().getConfigConfig().getString("server-name");

        Sanction sanction = new Sanction(
                targetUuid,
                offlineTarget.getName(),
                sender.getUniqueId(),
                sender.getName(),
                serverName,
                category,
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
}

