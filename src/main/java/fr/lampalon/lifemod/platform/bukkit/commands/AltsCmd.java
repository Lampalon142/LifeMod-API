package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.database.DatabaseProvider;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.model.PlayerData;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AltsCmd extends LifeCommand {

    public AltsCmd() {
        super("alts", "lifemod.alts", false);
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        fr.lampalon.lifemod.common.service.ILangService lang = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);

        if (args.length < 1) {
            sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("gui.alts.usage", "&cUsage: /alts <player/ip>")));
            return;
        }

        String input = args[0];
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("LifeMod"), () -> {
            String ip;
            if (input.contains(".")) {
                ip = input;
            } else {
                UUID uuid = Bukkit.getOfflinePlayer(input).getUniqueId();
                PlayerData data = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider().getPlayerData(uuid);
                if (data == null) {
                    sender.sendMessage(fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil.formatMessage(lang.getMessage("gui.alts.not-found", "&cThis player has never been recorded.")));
                    return;
                }
                ip = data.getLastIp();
            }

            List<PlayerData> alts = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider().getAlts(ip);

            if (sender.isPlayer()) {
                org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(sender.getUniqueId());
                if (player != null) {
                    new fr.lampalon.lifemod.platform.bukkit.gui.AltsGui(player, alts, ip).open();
                    return;
                }
            }

            sender.sendMessage(lang.getMessage("alts.header", "%ip%", ip));

            fr.lampalon.lifemod.common.service.ISanctionService sanctionService = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.service.ISanctionService.class);

            for (PlayerData alt : alts) {
                String color = "&7"; // Offline by default
                if (Bukkit.getPlayer(alt.getUuid()) != null) {
                    color = "&a"; // Online
                }
                
                // Check if banned
                fr.lampalon.lifemod.common.model.Sanction ban = sanctionService.getActiveSanction(alt.getUuid(), alt.getLastName(), fr.lampalon.lifemod.common.model.SanctionType.BAN).join();
                if (ban != null) {
                    color = "&c"; // Banned
                }

                sender.sendMessage(lang.getMessage("alts.entry", 
                    "%color%", color, 
                    "%name%", alt.getLastName(), 
                    "%uuid%", alt.getUuid().toString().substring(0,8)));
            }
            sender.sendMessage(lang.getMessage("alts.footer"));
        });
    }
}

