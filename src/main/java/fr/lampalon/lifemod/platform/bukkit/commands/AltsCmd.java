package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.platform.bukkit.managers.database.DatabaseProvider;.commands;

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
        if (args.length < 1) {
            sender.sendMessage("&cUsage: /alts [joueur/ip]");
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
                    sender.sendMessage("&cCe joueur n'a jamais été enregistré.");
                    return;
                }
                ip = data.getLastIp();
            }

            List<PlayerData> alts = LifeMod.getInstance().getDatabaseManager().getDatabaseProvider().getAlts(ip);
            
            sender.sendMessage("&8&m----------------------------------------");
            sender.sendMessage("&6Comptes liés pour l'IP: &e" + ip);
            sender.sendMessage(" ");
            for (PlayerData alt : alts) {
                String color = Bukkit.getPlayer(alt.getUuid()) != null ? "&a" : "&7";
                sender.sendMessage(" &8• " + color + alt.getLastName() + " &8(&7" + alt.getUuid().toString().substring(0,8) + "&8)");
            }
            sender.sendMessage("&8&m----------------------------------------");
        });
    }
}

