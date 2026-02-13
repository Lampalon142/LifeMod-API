package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CpsAction implements IStaffAction {

    private final Map<UUID, UUID> activeTests = new HashMap<>();

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.no-target")));
    }

    @Override
    public void onInteractEntity(Player player, PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player target = (Player) event.getRightClicked();

        if (activeTests.containsKey(player.getUniqueId())) {
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.already-active")));
            return;
        }

        int duration = 10; // Could be from config
        player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.start")
                .replace("%duration%", String.valueOf(duration))
                .replace("%target%", target.getName())));

        activeTests.put(player.getUniqueId(), target.getUniqueId());

        Bukkit.getScheduler().runTaskLater(LifeMod.getInstance(), () -> {
            activeTests.remove(player.getUniqueId());
            if (!player.isOnline()) return;

            Deque<Long> clicks = LifeMod.getInstance().getCpsMap().get(target.getUniqueId());
            if (clicks == null || clicks.isEmpty()) {
                player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.finish-none")
                        .replace("%target%", target.getName())));
                return;
            }

            long now = System.currentTimeMillis();
            java.util.List<Long> clicksDuringTest = clicks.stream()
                    .filter(t -> now - t <= duration * 1000L)
                    .collect(java.util.stream.Collectors.toList());

            double cps = (double) clicksDuringTest.size() / duration;
            boolean suspectedAutoClicker = cps > 15.0; // Simple threshold

            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.result-header")));
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.result-title").replace("%target%", target.getName())));
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.result-avg").replace("%cps%", String.format("%.2f", cps))));
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.result-total").replace("%clicks%", String.valueOf(clicksDuringTest.size()))));
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.result-autoclicker")
                    .replace("%result%", suspectedAutoClicker ? "&c&lSUSPECTED" : "&aUnlikely")));
            player.sendMessage(MessageUtil.formatMessage(LifeMod.getInstance().getLangConfig().getString("mod.items.cps.result-footer")));

        }, duration * 20L);
    }
}
