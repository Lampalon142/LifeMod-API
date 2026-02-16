package fr.lampalon.lifemod.platform.bukkit.commands;

import fr.lampalon.lifemod.common.commands.framework.ICommandSender;
import fr.lampalon.lifemod.common.commands.framework.LifeCommand;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class AltCmd extends LifeCommand {

    public AltCmd() {
        super("alt", "lifemod.antialt.check", false);
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        ILangService lang = ServiceRegistry.get(ILangService.class);
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.usage")));
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        String ip = "127.0.0.1";
        if (target != null && target.getAddress() != null) {
            ip = target.getAddress().getAddress().getHostAddress();
        }

        sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.analyzing", "%player%", targetName)));

        LifeMod.getInstance().getAntiAltManager().getEngine().analyze(targetName, ip).thenAccept(result -> {
            String rules = result.getTriggeredRules().isEmpty() 
                ? lang.getMessage("antialt.no-rules") 
                : result.getTriggeredRules().stream()
                    .map(r -> lang.getMessage("antialt.rules." + r.getKey()))
                    .collect(Collectors.joining("&7, "));

            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.header")));
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.title", "%player%", result.getPlayerName())));
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.score", 
                "%color%", getScoreColor(result.getDangerScore()), 
                "%score%", String.valueOf(result.getDangerScore()))));
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.rules-triggered", "%rules%", rules)));
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.fingerprint", "%fingerprint%", result.getFingerprint())));
            sender.sendMessage(MessageUtil.formatMessage(lang.getMessage("antialt.footer")));
        });
    }

    private String getScoreColor(int score) {
        if (score < 30) return "&a";
        if (score < 60) return "&e";
        if (score < 85) return "&c";
        return "&4&l";
    }
}
