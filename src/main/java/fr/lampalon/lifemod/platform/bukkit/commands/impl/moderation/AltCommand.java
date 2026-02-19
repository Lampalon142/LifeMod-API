package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
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
        String[] args = context.getArgs();
        if (args.length < 1) {
            context.getSender().sendMessage(context.getLang().getMessage("antialt.usage"));
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        String ip = "127.0.0.1";
        if (target != null && target.getAddress() != null) {
            ip = target.getAddress().getAddress().getHostAddress();
        }

        context.getSender().sendMessage(context.getLang().getMessage("antialt.analyzing", "%player%", targetName));

        LifeMod.getInstance().getAntiAltManager().getEngine().analyze(targetName, ip).thenAccept(result -> {
            String rules = result.getTriggeredRules().isEmpty() 
                ? context.getLang().getMessage("antialt.no-rules") 
                : result.getTriggeredRules().stream()
                    .map(r -> context.getLang().getMessage("antialt.rules." + r.getKey()))
                    .collect(Collectors.joining(context.getLang().getMessage("antialt.rule-separator")));

            context.getSender().sendMessage(context.getLang().getMessage("antialt.header"));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.title", "%player%", result.getPlayerName()));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.score", 
                "%color%", getScoreColor(result.getDangerScore()), 
                "%score%", String.valueOf(result.getDangerScore())));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.rules-triggered", "%rules%", rules));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.fingerprint", "%fingerprint%", result.getFingerprint()));
            context.getSender().sendMessage(context.getLang().getMessage("antialt.footer"));
        });
    }

    private String getScoreColor(int score) {
        if (score < 30) return "&a";
        if (score < 60) return "&e";
        return "&c";
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }
}
