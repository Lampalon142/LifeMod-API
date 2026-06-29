package fr.lampalon.lifemod.common.antivpn.provider.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.lampalon.lifemod.common.antivpn.data.IPInfo;
import fr.lampalon.lifemod.common.antivpn.provider.IPLookupProvider;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class IPApiProvider implements IPLookupProvider {
    private static final Logger LOGGER = Logger.getLogger(IPApiProvider.class.getName());

    @Override
    public CompletableFuture<IPInfo> lookup(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("http://ip-api.com/json/" + ip + "?fields=status,country,countryCode,isp,proxy,hosting");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        if ("success".equals(json.get("status").getAsString())) {
                            String country = json.get("country").getAsString();
                            String countryCode = json.get("countryCode").getAsString();
                            String isp = json.get("isp").getAsString();
                            boolean proxy = json.get("proxy").getAsBoolean() || json.get("hosting").getAsBoolean();
                            return new IPInfo(ip, countryCode, country, isp, proxy, System.currentTimeMillis());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("IP-API lookup failed for " + ip + ": " + e.getMessage());
            }
            return null;
        });
    }

    @Override
    public String getName() {
        return "IP-API";
    }
}
