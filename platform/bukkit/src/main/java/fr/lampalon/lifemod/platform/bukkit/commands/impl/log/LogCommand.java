package fr.lampalon.lifemod.platform.bukkit.commands.impl.log;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.LogQuery;
import fr.lampalon.lifemod.common.model.LogType;
import fr.lampalon.lifemod.common.service.ILogService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.gui.LogLookupGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogCommand extends LifeCommand {

    public LogCommand() {
        super("log", "lifemod.log", true);
        setDescription("Lookup player action logs.");
        setUsage("/log lookup <player> [type] [page]");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("logs.usage"));
            return;
        }

        String sub = context.getArgs()[0].toLowerCase();

        if (sub.equals("lookup") || sub.equals("l")) {
            executeLookup(context);
        } else {
            context.getSender().sendMessage(context.getLang().getMessage("logs.usage"));
        }
    }

    private void executeLookup(CommandContext context) {
        if (context.getArgs().length < 2) {
            context.getSender().sendMessage(context.getLang().getMessage("logs.usage"));
            return;
        }

        String targetName = context.getArgs()[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUuid = target.getUniqueId();

        LogType filterType;
        if (context.getArgs().length >= 3) {
            try {
                filterType = LogType.valueOf(context.getArgs()[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                context.getSender().sendMessage(context.getLang().getMessage("logs.invalid-type"));
                return;
            }
        } else {
            filterType = null;
        }
        final LogType finalFilterType = filterType;

        int page;
        if (context.getArgs().length >= 4) {
            try {
                page = Math.max(1, Integer.parseInt(context.getArgs()[3]));
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        } else {
            page = 1;
        }
        final int finalPage = page;

        ILogService logService = ServiceRegistry.get(ILogService.class);
        if (logService == null) {
            context.getSender().sendMessage(context.getLang().getMessage("logs.disabled"));
            return;
        }

        long from = System.currentTimeMillis() - 7L * 86400000L; // 7 days default
        LogQuery query = LogQuery.builder()
                .playerUuid(targetUuid)
                .type(filterType)
                .fromTime(from)
                .limit(45)
                .offset((page - 1) * 45)
                .build();

        logService.count(query).thenAccept(count -> {
            logService.query(query).thenAccept(entries -> {
                Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    new LogLookupGui(context.getPlayer(), entries, count, targetName, targetUuid, finalFilterType, finalPage).open();
                });
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return Stream.of("lookup", "l")
                    .filter(s -> s.startsWith(context.getArgs()[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (context.getArgs().length == 2) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[1]);
        }
        if (context.getArgs().length == 3) {
            String prefix = context.getArgs()[2].toUpperCase();
            return Stream.of(LogType.values())
                    .map(Enum::name)
                    .filter(n -> n.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return super.onTabComplete(context);
    }
}
