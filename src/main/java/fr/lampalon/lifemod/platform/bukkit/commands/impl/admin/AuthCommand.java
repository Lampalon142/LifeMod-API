package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;

import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorSessionManager;
import org.bukkit.entity.Player;

public class AuthCommand extends LifeCommand {
    private ModeratorSessionManager sessionManager;
    private ModeratorAuthService authService;
    private Player player;

    public AuthCommand(){
        super("auth", "lifemod.auth", false);
        setDescription("Auth command");
        setUsage("/auth <login|register|changepass|reset>");
    }


    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length == 0){
            //UsageMenu(context);
            return;
        }

        if (context.getArgs()[0].equalsIgnoreCase("login")){
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
        }
    }
}