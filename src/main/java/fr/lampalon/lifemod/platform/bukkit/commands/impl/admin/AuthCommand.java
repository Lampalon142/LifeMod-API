package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorSessionManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AuthCommand extends LifeCommand {

    public AuthCommand(){
        super("auth", "lifemod.auth", false);
        setDescription("Auth command");
        setUsage("/auth <login|register|changepass|reset>");
    }


    @Override
    public void execute(CommandContext context) {
        ModeratorSessionManager sessionManager = context.getPlugin().getModeratorSessionManager();
        ModeratorAuthService authService = context.getPlugin().getModeratorAuthService();
        Player player = context.getPlayer();

        if (context.getArgs().length == 0){
            //UsageMenu(context);
            return;
        }

        if (context.getArgs()[0].equalsIgnoreCase("login")){
            if (!context.isPlayer()){
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }

            if (sessionManager.isAuthenticated(player.getUniqueId())) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.already-authenticated"));
                return;
            }

            if (context.getArgs().length != 2) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-usage"));
                return;
            }

            String password = context.getArgs()[1];

            if (authService.checkPassword(player.getUniqueId(), password)) {
                sessionManager.authenticate(player.getUniqueId());
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-success"));
            } else {
                int attemptsLeft = sessionManager.decrementAttempts(player.getUniqueId());
                if (attemptsLeft <= 0) {
                    sessionManager.lock(player.getUniqueId());
                    context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-locked"));
                } else {
                    context.getSender().sendMessage(context.getLang().getMessage("commands.auth.login-failed", "%attempts%", String.valueOf(attemptsLeft)));
                }
            }
        }else if (context.getArgs()[0].equalsIgnoreCase("register")){
            if (!context.isPlayer()){
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }

            if (authService.isRegistered(player.getUniqueId())) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.already-registered"));
                return;
            }

            if (context.getArgs().length != 2) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.register-usage"));
                return;
            }

            String password = context.getArgs()[1];
            String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "UNKNOWN";

            authService.registerModerator(player.getUniqueId(), player.getName(), password, ip);
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.registered"));
        } else if (context.getArgs()[0].equalsIgnoreCase("changepass")){
            if (!context.isPlayer()){
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }

            if (context.getArgs().length != 3) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.changepass-usage"));
                return;
            }

            String oldPass = context.getArgs()[1];
            String newPass = context.getArgs()[2];

            if (!authService.checkPassword(player.getUniqueId(), oldPass)) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.incorrect-old-password"));
                return;
            }

            authService.changePassword(player.getUniqueId(), newPass);
            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.password-changed"));
        } else if (context.getArgs()[0].equalsIgnoreCase("reset")){
            if (context.getArgs().length != 2) {
                context.getSender().sendMessage(context.getLang().getMessage("commands.auth.reset-usage"));
                return;
            }

            String targetName = context.getArgs()[1];
            UUID targetUUID = authService.getUUIDByName(targetName);

            if (targetUUID == null || !authService.isRegistered(targetUUID)) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            authService.resetModeratorPassword(targetUUID);

            context.getSender().sendMessage(context.getLang().getMessage("commands.auth.reset-success", "%player%", targetName));
        }
    }
}