package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.model.Sanction;
import fr.lampalon.lifemod.common.model.SanctionType;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.common.utils.TimeUtil;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

public class LookupCommand extends LifeCommand {

    public LookupCommand() {
        super("lookup", "lifemod.lookup", false);
        setDescription("Look up detailed information about a player.");
        setUsage("/lookup <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("lookup.usage"));
            return;
        }

        String input = context.getArgs()[0];
        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        UUID uuid = offline.getUniqueId();
        String resolvedName = offline.getName() != null ? offline.getName() : input;

        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), () -> {
            DatabaseProvider db = context.getPlugin().getDatabaseManager().getDatabaseProvider();
            PlayerData data = db.getPlayerData(uuid);
            if (data == null) {
                context.getSender().sendMessage(context.getLang().getMessage("lookup.not-found", "%player%", input));
                return;
            }

            ILangService lang = context.getLang();
            CommandSender sender = context.getSender();
            LifeMod plugin = context.getPlugin();

            String status = lang.getMessage("lookup.status-offline");
            boolean redisEnabled = context.getConfig().getBoolean("redis.enabled", false);
            if (Bukkit.getPlayer(uuid) != null) {
                status = redisEnabled
                        ? composeStatus(lang, plugin.getServerName())
                        : lang.getMessage("lookup.status-online");
            } else if (redisEnabled) {
                String server = plugin.getPlayerServer(uuid);
                if (server != null) {
                    status = composeStatus(lang, server);
                }
            }

            List<Sanction> history = ServiceRegistry.get(ISanctionService.class).getHistory(uuid).join();
            long total = history.size();
            long warns = history.stream().filter(s -> s.getType() == SanctionType.WARN).count();
            long mutes = history.stream().filter(s -> s.getType() == SanctionType.MUTE).count();
            long bans = history.stream().filter(s -> s.getType() == SanctionType.BAN).count();
            long kicks = history.stream().filter(s -> s.getType() == SanctionType.KICK).count();
            long activeCount = history.stream().filter(Sanction::isActive).count();

            Sanction activeBan = history.stream()
                    .filter(s -> s.getType() == SanctionType.BAN && s.isActive())
                    .findFirst().orElse(null);

            String firstSeen = TimeUtil.formatTime(System.currentTimeMillis() - data.getFirstSeen());
            String lastSeen = data.getLastSeen() == 0
                    ? lang.getMessage("lookup.never")
                    : TimeUtil.formatTime(System.currentTimeMillis() - data.getLastSeen());

            String finalStatus = status;
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(lang.getMessage("lookup.header", "%player%", resolvedName));
                sender.sendMessage(lang.getMessage("lookup.ip", "%ip%", data.getLastIp() != null ? data.getLastIp() : "-"));
                sender.sendMessage(lang.getMessage("lookup.status", "%status%", finalStatus));
                sender.sendMessage(lang.getMessage("lookup.first-seen", "%time%", firstSeen));
                sender.sendMessage(lang.getMessage("lookup.last-seen", "%time%", lastSeen));
                sender.sendMessage(lang.getMessage("lookup.sessions", "%count%", String.valueOf(data.getSessionCount())));
                sender.sendMessage(lang.getMessage("lookup.sanctions",
                        "%total%", String.valueOf(total),
                        "%warn%", String.valueOf(warns),
                        "%mute%", String.valueOf(mutes),
                        "%ban%", String.valueOf(bans),
                        "%kick%", String.valueOf(kicks)));
                sender.sendMessage(lang.getMessage("lookup.active", "%count%", String.valueOf(activeCount)));
                if (activeBan != null) {
                    sender.sendMessage(lang.getMessage("lookup.active-ban",
                            "%reason%", activeBan.getReason(),
                            "%issuer%", activeBan.getIssuerName(),
                            "%time%", activeBan.isPermanent()
                                    ? lang.getMessage("sanctions.permanent")
                                    : TimeUtil.formatTime(activeBan.getExpirationTime() - System.currentTimeMillis())));
                }
                sender.sendMessage(lang.getMessage("lookup.footer"));
            });
        });
    }

    private String composeStatus(ILangService lang, String server) {
        return lang.getMessage("lookup.status-online-server", "%server%", server);
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}