package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.context.StaffActionContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CpsAction implements IStaffAction {

    private final Map<UUID, UUID> activeTests = new HashMap<>();

    @Override
    public void execute(StaffActionContext context) {
        Player player = context.getPlayer();
        ILangService lang = ServiceRegistry.get(ILangService.class);

        if (!context.hasEntityTarget() || !(context.getTargetEntity() instanceof Player)) {
            player.sendMessage(lang.getMessage("mod.items.no-target"));
            return;
        }

        Player target = (Player) context.getTargetEntity();

        if (activeTests.containsKey(player.getUniqueId())) {
            player.sendMessage(lang.getMessage("mod.items.cps.already-active"));
            return;
        }

        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        int duration = (int) config.getDouble("modules.mod-mode.items.cpstester.duration", 10.0);
        player.sendMessage(lang.getMessage("mod.items.cps.start",
                "%duration%", String.valueOf(duration),
                "%target%", target.getName()));

        activeTests.put(player.getUniqueId(), target.getUniqueId());

        Bukkit.getScheduler().runTaskLater(LifeMod.getInstance(), () -> {
            activeTests.remove(player.getUniqueId());
            if (!player.isOnline()) return;

            Deque<Long> clicks = LifeMod.getInstance().getCpsMap().get(target.getUniqueId());
            if (clicks == null || clicks.isEmpty()) {
                player.sendMessage(lang.getMessage("mod.items.cps.finish-none", "%target%", target.getName()));
                return;
            }

            long now = System.currentTimeMillis();
            List<Long> clicksDuringTest = clicks.stream()
                    .filter(t -> now - t <= duration * 1000L)
                    .collect(Collectors.toList());

            double cps = (double) clicksDuringTest.size() / duration;
            double threshold = config.getDouble("modules.mod-mode.items.cpstester.autoclicker-threshold", 15.0);
            boolean suspectedAutoClicker = cps > threshold;

            String statusKey = suspectedAutoClicker ? "mod.items.cps.status.suspected" : "mod.items.cps.status.unlikely";
            String resultStatus = lang.getMessage(statusKey);

            player.sendMessage(lang.getMessage("mod.items.cps.result-header"));
            player.sendMessage(lang.getMessage("mod.items.cps.result-title", "%target%", target.getName()));
            player.sendMessage(lang.getMessage("mod.items.cps.result-avg", "%cps%", String.format("%.2f", cps)));
            player.sendMessage(lang.getMessage("mod.items.cps.result-total", "%clicks%", String.valueOf(clicksDuringTest.size())));
            player.sendMessage(lang.getMessage("mod.items.cps.result-autoclicker", "%result%", resultStatus));
            player.sendMessage(lang.getMessage("mod.items.cps.result-footer"));
        }, duration * 20L);
    }
}
