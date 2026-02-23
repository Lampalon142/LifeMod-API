package fr.lampalon.lifemod.platform.bukkit.commands.impl.moderation;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.commands.api.CommandContext;
import fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand;
import fr.lampalon.lifemod.platform.bukkit.commands.utils.TabCompleterUtils;
import fr.lampalon.lifemod.platform.bukkit.utils.ActionBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FollowCommand extends LifeCommand {
    private final Map<UUID, UUID> following = new HashMap<>(); 
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>(); 

    public FollowCommand() {
        super("follow", "lifemod.follow", true);
        setDescription("Follows a player, displaying their distance and CPS.");
        setUsage("/follow <player>");
    }

    @Override
    public void execute(CommandContext context) {
        if (context.getArgs().length != 1) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.follow.usage"));
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(context.getArgs()[0]);
        if (targetPlayer == null) {
            context.getSender().sendMessage(context.getLang().getMessage("system.player-not-found"));
            return;
        }

        if (targetPlayer.equals(context.getPlayer())) {
            context.getSender().sendMessage(context.getLang().getMessage("commands.follow.yourself"));
            return;
        }

        if (tasks.containsKey(context.getSenderUniqueId())) {
            stopFollowing(context.getPlayer());
        }

        following.put(context.getSenderUniqueId(), targetPlayer.getUniqueId());
        context.getSender().sendMessage(context.getLang().getMessage("commands.follow.success", "%target%", targetPlayer.getName()));
        context.getDebug().log("follow", context.getSender().getName() + " is now following " + targetPlayer.getName());

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!context.getPlayer().isOnline() || !targetPlayer.isOnline() || !following.containsKey(context.getSenderUniqueId())) {
                    stopFollowing(context.getPlayer());
                    this.cancel();
                    return;
                }
                
                double distance = context.getPlayer().getLocation().distance(targetPlayer.getLocation());
                int cps = getCPS(targetPlayer, context);

                String msg = context.getLang().getMessage("commands.follow.actionbar")
                                .replace("%target%", targetPlayer.getName())
                                .replace("%distance%", String.format("%.1f", distance))
                                .replace("%cps%", String.valueOf(cps));

                ActionBarUtil.sendActionBar(context.getPlayer(), msg);
            }
        };
        task.runTaskTimer(context.getPlugin(), 0L, 20L);
        tasks.put(context.getSenderUniqueId(), task);
    }

    @Override
    public List<String> onTabComplete(CommandContext context) {
        if (context.getArgs().length == 1) {
            return TabCompleterUtils.filterOnlinePlayers(context.getArgs()[0]);
        }
        return super.onTabComplete(context);
    }

    private int getCPS(Player player, CommandContext context) {
        Deque<Long> deque = context.getPlugin().getCpsMap().get(player.getUniqueId());
        if (deque == null) return 0;
        long now = System.currentTimeMillis();
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000) {
            deque.pollFirst();
        }
        return deque.size();
    }

    public void stopFollowing(Player follower) {
        following.remove(follower.getUniqueId());
        if (tasks.containsKey(follower.getUniqueId())) {
            tasks.get(follower.getUniqueId()).cancel();
            tasks.remove(follower.getUniqueId());
            follower.sendMessage(ServiceRegistry.get(ILangService.class).getMessage("commands.follow.stopped"));
        }
    }
}
