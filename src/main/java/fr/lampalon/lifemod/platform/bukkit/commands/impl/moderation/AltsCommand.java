package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.common.service.ISanctionService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.gui.AltsGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class AltsCommand extends LifeCommand {

    public AltsCommand() {
        super("alts", "lifemod.alts", false);
        setDescription("View a player's alternative accounts.");
        setUsage("/alts <player/ip>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("gui.alts.usage"));
            return;
        }

        String input = context.getArgs()[0];
        Bukkit.getScheduler().runTaskAsynchronously(context.getPlugin(), () -> {
            String ip;
            if (input.contains(".")) {
                ip = input;
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(input);
                UUID uuid = offlinePlayer.getUniqueId();
                PlayerData data = context.getPlugin().getDatabaseManager().getDatabaseProvider().getPlayerData(uuid);
                if (data == null) {
                    context.getSender().sendMessage(context.getLang().getMessage("gui.alts.not-found"));
                    return;
                }
                ip = data.getLastIp();
            }

            List<PlayerData> alts = context.getPlugin().getDatabaseManager().getDatabaseProvider().getAlts(ip);

            if (context.isPlayer()) {
                Bukkit.getScheduler().runTask(context.getPlugin(), () -> {
                    new AltsGui(context.getPlayer(), alts, ip).open();
                });
                return;
            }

            context.getSender().sendMessage(context.getLang().getMessage("alts.header", "%ip%", ip));

            ISanctionService sanctionService = ServiceRegistry.get(ISanctionService.class);

            for (PlayerData alt : alts) {
                String color = context.getLang().getMessage("gui.alts.status-offline-color"); 
                if (Bukkit.getPlayer(alt.getUuid()) != null) {
                    color = context.getLang().getMessage("gui.alts.status-online-color"); 
                }
                
                if (sanctionService.getActiveSanction(alt.getUuid(), alt.getLastName(), fr.lampalon.lifemod.common.model.SanctionType.BAN).join() != null) {
                    color = context.getLang().getMessage("gui.alts.status-banned-color"); 
                }

                context.getSender().sendMessage(context.getLang().getMessage("alts.entry", 
                    "%color%", color, 
                    "%name%", alt.getLastName(), 
                    "%uuid%", alt.getUuid().toString().substring(0,8)));
            }
            context.getSender().sendMessage(context.getLang().getMessage("alts.footer"));
        });
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
