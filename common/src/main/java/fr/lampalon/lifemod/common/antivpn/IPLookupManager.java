package fr.lampalon.lifemod.common.antivpn;

import fr.lampalon.lifemod.common.antivpn.data.IPInfo;
import fr.lampalon.lifemod.common.antivpn.provider.IPLookupProvider;
import fr.lampalon.lifemod.common.antivpn.provider.impl.IPApiProvider;
import fr.lampalon.lifemod.common.core.ServiceRegistry;
import fr.lampalon.lifemod.common.database.DatabaseProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class IPLookupManager {

    private final List<IPLookupProvider> providers = new ArrayList<>();
    private final Map<String, CompletableFuture<IPInfo>> pendingLookups = new ConcurrentHashMap<>();
    private final long cacheExpiryTime = 7 * 24 * 60 * 60 * 1000L; // 7 days

    public IPLookupManager() {
        // Register default providers
        providers.add(new IPApiProvider());
    }

    public CompletableFuture<IPInfo> lookup(String ip) {
        // 1. Check pending lookups to avoid duplicate requests
        if (pendingLookups.containsKey(ip)) {
            return pendingLookups.get(ip);
        }

        CompletableFuture<IPInfo> future = new CompletableFuture<>();
        pendingLookups.put(ip, future);

        // 2. Check Database Cache
        DatabaseProvider db = ServiceRegistry.get(DatabaseProvider.class);
        IPInfo cached = db != null ? db.getIPInfo(ip) : null;

        if (cached != null && (System.currentTimeMillis() - cached.getLastUpdate()) < cacheExpiryTime) {
            future.complete(cached);
            pendingLookups.remove(ip);
            return future;
        }

        // 3. Sequential Provider Lookup
        lookupFromProviders(ip, 0, future);

        return future;
    }

    private void lookupFromProviders(String ip, int index, CompletableFuture<IPInfo> future) {
        if (index >= providers.size()) {
            future.complete(null);
            pendingLookups.remove(ip);
            return;
        }

        providers.get(index).lookup(ip).thenAccept(info -> {
            try {
                if (info != null) {
                    // Save to DB Cache
                    DatabaseProvider db = ServiceRegistry.get(DatabaseProvider.class);
                    if (db != null) {
                        db.saveIPInfo(info.getIp(), info.getCountryCode(), info.getCountryName(), info.getIsp(), info.isProxy(), info.getLastUpdate());
                    }
                    future.complete(info);
                } else {
                    // Try next provider
                    lookupFromProviders(ip, index + 1, future);
                }
            } finally {
                if (info != null) pendingLookups.remove(ip);
            }
        }).exceptionally(ex -> {
            lookupFromProviders(ip, index + 1, future);
            return null;
        });
    }

    public void addProvider(IPLookupProvider provider) {
        providers.add(0, provider); // Add new providers at the beginning for priority
    }
}
