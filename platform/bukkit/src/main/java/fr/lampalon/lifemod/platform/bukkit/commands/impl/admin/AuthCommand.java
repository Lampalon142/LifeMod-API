package fr.lampalon.lifemod.platform.bukkit.commands.impl.admin;


import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorAuthService;
import fr.lampalon.lifemod.platform.bukkit.managers.ModeratorSessionManager;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class AuthCommand extends LifeCommand {

    public AuthCommand(){
        super("auth", "lifemod.auth", false);
        setDescription("Auth command");
                        setUsage("/auth <login|register|changepass|reset|info|set|list|lock|unlock>");
    }


    @Override
    public void execute(CommandContext context) {
        ModeratorSessionManager sessionManager = context.getPlugin().getModeratorSessionManager();
        ModeratorAuthService authService = context.getPlugin().getModeratorAuthService();
        if (sessionManager == null || authService == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.module-disabled"));
            return;
        }
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
                context.getSender().sendMessage(context.getLang().getMessage("auth.already-authenticated"));
                return;
            }

            new fr.lampalon.lifemod.platform.bukkit.gui.PinGui(player).open();
        }else if (context.getArgs()[0].equalsIgnoreCase("register")){
            if (!context.isPlayer()){
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }

            if (authService.isRegistered(player.getUniqueId())) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.already-registered"));
                return;
            }

            new fr.lampalon.lifemod.platform.bukkit.gui.PinGui(player).open();
        } else if (context.getArgs()[0].equalsIgnoreCase("changepass")){
            if (!context.isPlayer()){
                context.getSender().sendMessage(context.getLang().getMessage("system.player-only"));
                return;
            }

            if (context.getArgs().length != 3) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.changepass-usage"));
                return;
            }

            String oldPass = context.getArgs()[1];
            String newPass = context.getArgs()[2];

            if (!authService.checkPassword(player.getUniqueId(), oldPass)) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.incorrect-old-password"));
                return;
            }

            authService.changePassword(player.getUniqueId(), newPass);
            context.getSender().sendMessage(context.getLang().getMessage("auth.password-changed"));
        } else if (context.getArgs()[0].equalsIgnoreCase("reset")){
            if (context.getArgs().length != 2) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.reset-usage"));
                return;
            }

            String targetName = context.getArgs()[1];
            UUID targetUUID = authService.getUUIDByName(targetName);

            if (targetUUID == null || !authService.isRegistered(targetUUID)) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            authService.resetModeratorPassword(targetUUID);

            context.getSender().sendMessage(context.getLang().getMessage("auth.reset-success", "%player%", targetName));
        } else if (context.getArgs()[0].equalsIgnoreCase("info")){
            if (context.getArgs().length != 2) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.admin-info-usage"));
                return;
            }

            String targetName = context.getArgs()[1];
            UUID targetUUID = authService.getUUIDByName(targetName);

            if (targetUUID == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            boolean registered = authService.isRegistered(targetUUID);
            if (!registered) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.admin-info-not-registered", "%player%", targetName));
                return;
            }

            long lastAuth = authService.getLastAuthTime(targetUUID);
            String lastIp = authService.getLastAuthIp(targetUUID);
            String lastAuthStr = lastAuth > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastAuth)) : context.getLang().getMessage("auth.admin-info-never");
            String ipStr = lastIp != null ? lastIp : context.getLang().getMessage("auth.admin-info-never");

            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-info-header", "%player%", targetName));
            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-info-registered"));
            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-info-last-auth", "%time%", lastAuthStr));
            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-info-last-ip", "%ip%", ipStr));
        } else if (context.getArgs()[0].equalsIgnoreCase("set")){
            if (context.getArgs().length != 3) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.admin-set-usage"));
                return;
            }

            String targetName = context.getArgs()[1];
            String pin = context.getArgs()[2];

            if (pin.length() < 4 || pin.length() > 8 || !pin.matches("\\d+")) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.admin-invalid-pin"));
                return;
            }

            UUID targetUUID = authService.getUUIDByName(targetName);
            if (targetUUID == null) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            authService.changePassword(targetUUID, pin);
            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-set-success", "%player%", targetName));
        } else if (context.getArgs()[0].equalsIgnoreCase("list")){
            java.util.List<String> names = authService.getAllRegisteredNames();
            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-list-header", "%count%", String.valueOf(names.size())));
            for (String name : names) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.admin-list-entry", "%name%", name));
            }
        } else if (context.getArgs()[0].equalsIgnoreCase("lock")){
            if (context.getArgs().length != 2) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.admin-lock-usage"));
                return;
            }

            String targetName = context.getArgs()[1];
            UUID targetUUID = authService.getUUIDByName(targetName);

            if (targetUUID == null || !authService.isRegistered(targetUUID)) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            sessionManager.lock(targetUUID);
            Player targetPlayer = org.bukkit.Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.kickPlayer(context.getLang().getMessage("auth.login-locked"));
            }
            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-lock-success", "%player%", targetName));
        } else if (context.getArgs()[0].equalsIgnoreCase("unlock")){
            if (context.getArgs().length != 2) {
                context.getSender().sendMessage(context.getLang().getMessage("auth.admin-unlock-usage"));
                return;
            }

            String targetName = context.getArgs()[1];
            UUID targetUUID = authService.getUUIDByName(targetName);

            if (targetUUID == null || !authService.isRegistered(targetUUID)) {
                context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
                return;
            }

            sessionManager.unlock(targetUUID);
            context.getSender().sendMessage(context.getLang().getMessage("auth.admin-unlock-success", "%player%", targetName));
        }
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length <= 1) {
            return List.of("login", "register", "changepass", "reset", "info", "set", "list", "lock", "unlock");
        }

        String sub = context.getArgs()[0].toLowerCase();
        if (List.of("reset", "info", "set", "lock", "unlock").contains(sub) && context.getArgs().length == 2) {
            ModeratorAuthService authService = context.getPlugin().getModeratorAuthService();
            if (authService != null) return authService.getAllRegisteredNames();
        }

        return List.of();
    }
}