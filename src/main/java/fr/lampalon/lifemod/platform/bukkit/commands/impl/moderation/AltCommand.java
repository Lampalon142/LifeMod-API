package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class AltCommand extends LifeCommand {

    public AltCommand() {
        super("alt", "lifemod.antialt.check", false);
        setDescription("Analyze a player for suspicious patterns (AntiAlt).");
        setUsage("/alt <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("antialt.usage"));
            return;
        }

        String targetName = context.getArgs()[0];
        Player target = Bukkit.getPlayer(targetName);
        
        java.util.UUID targetUuid;
        String ip = "127.0.0.1";

        if (target != null) {
            targetUuid = target.getUniqueId();
            if (target.getAddress() != null) {
                ip = target.getAddress().getAddress().getHostAddress();
            }
        } else {
            // Support pour les joueurs hors-ligne (si on a leurs données)
            fr.lampalon.lifemod.common.database.DatabaseProvider db = fr.lampalon.lifemod.common.core.ServiceRegistry.get(fr.lampalon.lifemod.common.database.DatabaseProvider.class);
            // On peut chercher l'UUID par le nom si on avait une méthode pour ça, sinon on utilise un placeholder ou on refuse.
            // Pour l'instant on se limite aux joueurs en ligne ou on cherche dans PlayerData si possible.
            // Comme le moteur a besoin de l'UUID pour l'historique, c'est crucial.
            context.getSender().sendMessage("§cNote: L'analyse de joueurs hors-ligne n'est supportée que s'ils ont déjà été connectés.");
            return; // Simplification pour l'instant
        }

        context.getSender().sendMessage(context.getLang().getMessage("antialt.analyzing", "%player%", targetName));

        context.getPlugin().getAntiAltManager().getEngine().analyze(targetUuid, targetName, ip).thenAccept(result -> {
            String rules = result.getTriggeredRules().isEmpty() 
                ? context.getLang().getMessage("antialt.no-rules") 
                : result.getTriggeredRules().stream()
                    .map(r -> context.getLang().getMessage("antialt.rules." + r.getKey()))
                    .collect(Collectors.joining(context.getLang().getMessage("antialt.rule-separator")));

            context.getSender().sendMessage(context.getLang().getMessage("antialt.header"));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.title", "%player%", result.getPlayerName()));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.score", 
                "%color%", getScoreColor(result.getDangerScore(), context),
                "%score%", String.valueOf(result.getDangerScore())));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.rules-triggered", "%rules%", rules));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.fingerprint", "%fingerprint%", result.getFingerprint()));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.footer"));
        });
    }

    private String getScoreColor(int score, CommandContext context) {
        if (score < 30) return context.getLang().getMessage("antialt.score.low");
        if (score < 60) return context.getLang().getMessage("antialt.score.medium");
        if (score < 85) return context.getLang().getMessage("antialt.score.high");
        return "&4&l";
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
