package fr.lampalon.lifemod.platform.bukkit.managers.antialt;

import fr.lampalon.lifemod.common.antialt.AnalysisResult;
import fr.lampalon.lifemod.common.antialt.HeuristicEngine;
import fr.lampalon.lifemod.common.antialt.HeuristicRule;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.utils.NetworkUtil;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AntiAltManager {

    private final LifeMod plugin;
    private final HeuristicEngine engine;
    private final Map<String, AnalysisResult> analysisCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamp = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 300_000; // 5 minutes

    public AntiAltManager(LifeMod plugin) {
        this.plugin = plugin;
        this.engine = new HeuristicEngine(ServiceRegistry.get(IConfigurationService.class));
    }

    public HeuristicEngine getEngine() {
        return engine;
    }

    public void handlePlayerJoin(Player player) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        if (config == null || !config.getBoolean("modules.antialt.enabled", true)) return;
        if (player.hasPermission("lifemod.antialt.bypass")) return;

        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1";
        
        // Check Cache
        if (analysisCache.containsKey(ip)) {
            long ts = cacheTimestamp.getOrDefault(ip, 0L);
            if (System.currentTimeMillis() - ts < CACHE_TTL) {
                processDecision(player, analysisCache.get(ip), ip);
                return;
            }
        }

        engine.analyze(player.getUniqueId(), player.getName(), ip).thenAccept(result -> {
            if (result == null) return;
            
            // Update Cache
            analysisCache.put(ip, result);
            cacheTimestamp.put(ip, System.currentTimeMillis());
            
            // 1. Log session
            DatabaseProvider db = ServiceRegistry.get(DatabaseProvider.class);
            if (db != null) {
                String subnet = NetworkUtil.getSubnet(ip);
                boolean vpnDetected = result.getTriggeredRules().contains(HeuristicRule.VPN_DETECTED);
                String flags = result.getTriggeredRules().stream().map(HeuristicRule::getKey).collect(Collectors.joining(","));
                db.logAltSession(player.getUniqueId(), ip, subnet, System.currentTimeMillis(), result.getDangerScore(), vpnDetected, flags);
            }

            // 2. Process Reactions based on Decision Matrix
            processDecision(player, result, ip);
        });
    }

    private void processDecision(Player player, AnalysisResult result, String ip) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        int score = result.getDangerScore();

        int logSilent = config.getInt("modules.antialt.thresholds.log-silent", 30);
        int alertAdmin = config.getInt("modules.antialt.thresholds.alert-admin", 50);
        int alertPriority = config.getInt("modules.antialt.thresholds.alert-priority", 70);
        int suggestBan = config.getInt("modules.antialt.thresholds.suggest-ban", 85);

        if (score >= suggestBan) {
            sendAuditAlert(player, result, ip, lang.getMessage("antialt.recommendation.ban"), true);
        } else if (score >= alertPriority) {
            sendAuditAlert(player, result, ip, lang.getMessage("antialt.recommendation.priority"), true);
        } else if (score >= alertAdmin) {
            sendAuditAlert(player, result, ip, lang.getMessage("antialt.recommendation.admin"), false);
        } else if (score >= logSilent) {
            plugin.getLogger().info("[AntiAlt] Log Silencieux - Joueur: " + player.getName() + " Score: " + score + "/100");
        }
    }

    private void sendAuditAlert(Player player, AnalysisResult result, String ip, String action, boolean priority) {
        fr.lampalon.lifemod.common.service.ILangService lang = ServiceRegistry.get(fr.lampalon.lifemod.common.service.ILangService.class);
        
        String signals = result.getTriggeredRules().stream()
                .map(rule -> lang.getMessage("antialt.signal-format", "%reason%", rule.getReason()))
                .collect(Collectors.joining("\n"));

        String message = lang.getMessage("antialt.audit-alert",
                "%player%", player.getName(),
                "%score_color%", getScoreColor(result.getDangerScore()),
                "%score%", String.valueOf(result.getDangerScore()),
                "%uuid%", player.getUniqueId().toString(),
                "%ip%", ip,
                "%signals%", signals,
                "%action%", action);

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("lifemod.antialt.alerts"))
                .forEach(p -> p.sendMessage(message));
        
        if (priority) {
            // Optionnel : Son pour les alertes prioritaires
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("lifemod.antialt.alerts"))
                    .forEach(p -> p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f));
        }
    }

    private String getScoreColor(int score) {
        if (score >= 85) return "§4§l";
        if (score >= 70) return "§c";
        if (score >= 50) return "§6";
        return "§e";
    }
}
