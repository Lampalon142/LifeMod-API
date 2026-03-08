package fr.lampalon.lifemod.platform.bukkit.commands.impl;

import fr.lampalon.lifemod.common.anticheat.AntiCheatService;
import fr.lampalon.lifemod.common.anticheat.data.ACPlayerData;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Commande de gestion de l'AntiCheat (Version compatible avec le nouveau moteur de commandes).
 */
public class ACCommand extends LifeCommand {

    private static final Set<UUID> alertsToggled = new HashSet<>();

    public ACCommand() {
        // name, permission, playerOnly, aliases
        super("ac", "lifemod.anticheat", false);
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.getArgs();
        
        if (args.length == 0) {
            context.getSender().sendMessage("§6§lLifeMod AntiCheat §7- §fCommandes :");
            context.getSender().sendMessage("§e/ac alerts §7- Activer/Désactiver les alertes.");
            context.getSender().sendMessage("§e/ac stats <joueur> §7- Voir les statistiques.");
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("alerts")) {
            if (!context.isPlayer()) {
                context.getSender().sendMessage("§cCette commande est réservée aux joueurs.");
                return;
            }
            UUID uuid = context.getSenderUniqueId();
            if (alertsToggled.contains(uuid)) {
                alertsToggled.remove(uuid);
                context.getSender().sendMessage("§6[Feat-AC] §fAlertes §cdésactivées.");
            } else {
                alertsToggled.add(uuid);
                context.getSender().sendMessage("§6[Feat-AC] §fAlertes §aactivées.");
            }
        } else if (sub.equals("stats")) {
            if (args.length < 2) {
                context.getSender().sendMessage("§cUsage: /ac stats <joueur>");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                context.getSender().sendMessage("§cJoueur introuvable.");
                return;
            }
            AntiCheatService service = ServiceRegistry.get(AntiCheatService.class);
            if (service == null) return;
            
            ACPlayerData data = service.getPlayerData(target.getUniqueId());
            if (data == null) {
                context.getSender().sendMessage("§cAucune donnée disponible pour ce joueur.");
                return;
            }

            context.getSender().sendMessage("§8§m----------------------------------------");
            context.getSender().sendMessage("§6§lAntiCheat Stats: §e" + target.getName());
            context.getSender().sendMessage("§fConfiance: §b" + String.format("%.2f", data.getConfidenceScore()));
            context.getSender().sendMessage("§fBalance Timer: §7" + data.getTimerBalance() + "ms");
            context.getSender().sendMessage("§fViolations :");
            data.getViolationLevels().forEach((check, vl) -> {
                context.getSender().sendMessage(" §7- §e" + check + " : §c" + vl + " VL");
            });
            context.getSender().sendMessage("§8§m----------------------------------------");
        } else {
            context.getSender().sendMessage("§cSous-commande inconnue.");
        }
    }

    public static boolean hasAlertsEnabled(UUID uuid) {
        return alertsToggled.contains(uuid);
    }
}
