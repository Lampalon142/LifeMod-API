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
            sender.sendMessage(MessageUtil.formatMessage("&cUsage: /alt <player>"));
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        String ip = "127.0.0.1";
        if (target != null && target.getAddress() != null) {
            ip = target.getAddress().getAddress().getHostAddress();
        }

        sender.sendMessage(MessageUtil.formatMessage("&6[AntiAlt] &eAnalyse de &b" + targetName + " &een cours..."));

        LifeMod.getInstance().getAntiAltManager().getEngine().analyze(targetName, ip).thenAccept(result -> {
            sender.sendMessage(MessageUtil.formatMessage("&8&m----------------------------------------"));
            sender.sendMessage(MessageUtil.formatMessage("&6&lAntiAlt Analysis: &b" + result.getPlayerName()));
            sender.sendMessage(MessageUtil.formatMessage("&e• &fScore de Danger: " + getScoreColor(result.getDangerScore()) + result.getDangerScore() + "/100"));
            sender.sendMessage(MessageUtil.formatMessage("&e• &fRègles déclenchées: &7" + (result.getTriggeredRules().isEmpty() ? "Aucune" : result.getTriggeredRules().stream().map(r -> "&c" + r.getKey()).collect(Collectors.joining("&7, ")))));
            sender.sendMessage(MessageUtil.formatMessage("&e• &fFingerprint: &7" + result.getFingerprint()));
            sender.sendMessage(MessageUtil.formatMessage("&8&m----------------------------------------"));
        });
    }

    private String getScoreColor(int score) {
        if (score < 30) return "&a";
        if (score < 60) return "&e";
        if (score < 85) return "&c";
        return "&4&l";
    }
}
