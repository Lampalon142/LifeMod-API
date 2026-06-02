package fr.lampalon.lifemod.common.antivpn;

import fr.lampalon.lifemod.common.antivpn.data.IPInfo;
import fr.lampalon.lifemod.common.core.ILifePlatform;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.service.IConfigurationService;
import fr.lampalon.lifemod.common.service.ILangService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AntiVPNService {

    private final ILifePlatform platform;
    private final IPLookupManager lookupManager;
    private final Map<String, AtomicInteger> connectionRateMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastConnectionTime = new ConcurrentHashMap<>();
    private final AtomicInteger globalConnectionsLastSecond = new AtomicInteger(0);

    public AntiVPNService(ILifePlatform platform) {
        this.platform = platform;
        this.lookupManager = new IPLookupManager();
        
        // Rate limiting reset task
        platform.runTaskAsync(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    globalConnectionsLastSecond.set(0);
                    connectionRateMap.clear();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public CompletableFuture<Boolean> shouldAllowConnection(String ip, String name) {
        IConfigurationService config = ServiceRegistry.get(IConfigurationService.class);
        if (config == null || !config.getBoolean("modules.antivpn.enabled", true)) {
            return CompletableFuture.completedFuture(true);
        }

        // 1. Whitelist Checks
        List<String> whitelist = config.getStringList("modules.antivpn.whitelist");
        if (whitelist.contains(ip) || whitelist.contains(name)) {
            return CompletableFuture.completedFuture(true);
        }

        ILangService lang = ServiceRegistry.get(ILangService.class);

        // 2. Anti-Bot: Global Rate Limit
        int globalLimit = config.getInt("modules.antivpn.anti-bot.global-limit", 5);
        if (globalConnectionsLastSecond.incrementAndGet() > globalLimit) {
            platform.logInfo(lang != null ? lang.getMessage("antivpn.log.rate-limit", "%player%", name, "%ip%", ip) : "§c[AntiVPN] Blocking connection (Global Rate Limit): " + name + " (" + ip + ")");
            return CompletableFuture.completedFuture(false);
        }

        // 3. Anti-Bot: Per-IP Rate Limit
        long now = System.currentTimeMillis();
        long lastTime = lastConnectionTime.getOrDefault(ip, 0L);
        int perIpLimit = config.getInt("modules.antivpn.anti-bot.per-ip-cooldown", 10000); // ms
        if (now - lastTime < perIpLimit) {
            platform.logInfo(lang != null ? lang.getMessage("antivpn.log.ip-cooldown", "%player%", name, "%ip%", ip) : "§c[AntiVPN] Blocking connection (IP Cooldown): " + name + " (" + ip + ")");
            return CompletableFuture.completedFuture(false);
        }
        lastConnectionTime.put(ip, now);

        // 4. IP Lookup Analysis
        ILangService finalLang = lang;
        return lookupManager.lookup(ip).thenApply(info -> {
            if (info == null) return true; // Fallback if lookup fails

            // A. VPN/Proxy Detection
            if (info.isProxy() && config.getBoolean("modules.antivpn.block-vpn", true)) {
                platform.logInfo(finalLang != null ? finalLang.getMessage("antivpn.log.vpn-proxy", "%player%", name, "%ip%", ip, "%isp%", info.getIsp()) : "§c[AntiVPN] Blocking VPN/Proxy: " + name + " (" + ip + ") - ISP: " + info.getIsp());
                return false;
            }

            // B. Geo-Blocking
            String mode = config.getString("modules.antivpn.geo-blocking.mode", "NONE");
            List<String> countries = config.getStringList("modules.antivpn.geo-blocking.countries");
            if (mode.equalsIgnoreCase("WHITELIST")) {
                if (!countries.contains(info.getCountryCode())) {
                    platform.logInfo(finalLang != null ? finalLang.getMessage("antivpn.log.geo-not-whitelisted", "%player%", name, "%ip%", ip, "%country%", info.getCountryCode()) : "§c[AntiVPN] Blocking Geo (Not Whitelisted): " + name + " (" + ip + ") - Country: " + info.getCountryCode());
                    return false;
                }
            } else if (mode.equalsIgnoreCase("BLACKLIST")) {
                if (countries.contains(info.getCountryCode())) {
                    platform.logInfo(finalLang != null ? finalLang.getMessage("antivpn.log.geo-blacklisted", "%player%", name, "%ip%", ip, "%country%", info.getCountryCode()) : "§c[AntiVPN] Blocking Geo (Blacklisted): " + name + " (" + ip + ") - Country: " + info.getCountryCode());
                    return false;
                }
            }

            // C. ISP Detection
            List<String> blockedISPs = config.getStringList("modules.antivpn.blocked-isps");
            for (String blockedIsp : blockedISPs) {
                if (info.getIsp().toLowerCase().contains(blockedIsp.toLowerCase())) {
                    platform.logInfo(finalLang != null ? finalLang.getMessage("antivpn.log.isp", "%player%", name, "%ip%", ip, "%isp%", info.getIsp()) : "§c[AntiVPN] Blocking ISP: " + name + " (" + ip + ") - ISP: " + info.getIsp());
                    return false;
                }
            }

            return true;
        });
    }

    public IPLookupManager getLookupManager() {
        return lookupManager;
    }
}
