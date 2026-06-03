package fr.lampalon.lifemod.common.analytics;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PostHogSetup {

    private static final String US_API = "https://us.posthog.com";
    private static final String EU_API = "https://eu.posthog.com";

    private final String baseUrl;
    private final String personalApiKey;
    private final String environmentId;

    public PostHogSetup(String region, String personalApiKey, String environmentId) {
        this.baseUrl = region.equalsIgnoreCase("eu") ? EU_API : US_API;
        this.personalApiKey = personalApiKey;
        this.environmentId = environmentId;
    }

    public static void main(String[] args) {
        System.out.println("=== LifeMod PostHog Auto-Setup ===");
        System.out.println();

        if (args.length < 2) {
            System.out.println("Usage: java PostHogSetup <region> <personal_api_key> [environment_id]");
            System.out.println();
            System.out.println("  region           : 'us' or 'eu' (your PostHog datacenter)");
            System.out.println("  personal_api_key : generate at https://app.posthog.com/settings/user-api-keys");
            System.out.println("  environment_id   : optional - your project's numeric ID");
            System.out.println("                     (find at Settings > Project > Project ID)");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  java PostHogSetup us phx_abc123def 12345");
            return;
        }

        String region = args[0];
        String personalApiKey = args[1];
        String environmentId = args.length > 2 ? args[2] : null;

        PostHogSetup setup = new PostHogSetup(region, personalApiKey, environmentId);

        try {
            setup.run();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void run() throws Exception {
        String envId = environmentId != null ? environmentId : fetchEnvironmentId();
        System.out.println("Using environment: " + envId);
        System.out.println("API base: " + baseUrl);
        System.out.println();

        Map<String, String> existingDashboards = fetchExistingDashboards();
        System.out.println("Found " + existingDashboards.size() + " existing dashboard(s)");
        System.out.println();

        List<Map<String, Object>> dashboards = defineDashboards();

        for (Map<String, Object> dashDef : dashboards) {
            String dashName = (String) dashDef.get("name");

            String dashId = existingDashboards.get(dashName);
            if (dashId != null) {
                System.out.println("Dashboard \"" + dashName + "\" already exists (ID=" + dashId + ")");
            } else {
                System.out.print("Creating dashboard \"" + dashName + "\"... ");
                dashId = createDashboard(dashName, (String) dashDef.get("description"), (List<String>) dashDef.get("tags"));
                if (dashId == null) {
                    System.out.println("FAILED");
                    continue;
                }
                System.out.println("ID=" + dashId);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> insights = (List<Map<String, Object>>) dashDef.get("insights");
            for (Map<String, Object> insightDef : insights) {
                String insightName = (String) insightDef.get("name");
                System.out.print("  Creating insight \"" + insightName + "\"... ");
                createInsight(insightName, dashId, insightDef);
                System.out.println("OK");
            }
            System.out.println();
        }

        System.out.println("=== Setup complete! ===");
        System.out.println("Refresh your PostHog dashboard page to see the new dashboards.");
    }

    private Map<String, String> fetchExistingDashboards() throws Exception {
        Map<String, String> result = new java.util.HashMap<>();
        String response = get(baseUrl + "/api/environments/" + environmentId + "/dashboards/");
        String searchKey = "\"results\":[";
        int idx = response.indexOf(searchKey);
        if (idx == -1) return result;

        int start = idx + searchKey.length();
        int end = response.lastIndexOf("]}");
        if (end == -1) end = response.length();

        String dashboardsJson = response.substring(start, end);
        int pos = 0;
        while (pos < dashboardsJson.length()) {
            int idPos = dashboardsJson.indexOf("\"id\":", pos);
            if (idPos == -1) break;
            int idStart = idPos + 5;
            int idEnd = dashboardsJson.indexOf(",", idStart);
            if (idEnd == -1) idEnd = dashboardsJson.indexOf("}", idStart);
            String id = dashboardsJson.substring(idStart, idEnd).trim();

            int namePos = dashboardsJson.indexOf("\"name\":", idPos);
            if (namePos == -1 || namePos > dashboardsJson.indexOf("}", idPos)) break;
            int nameStart = dashboardsJson.indexOf("\"", namePos + 7) + 1;
            int nameEnd = dashboardsJson.indexOf("\"", nameStart);
            String name = dashboardsJson.substring(nameStart, nameEnd);

            result.put(name, id);
            pos = nameEnd + 1;
        }
        return result;
    }

    private String fetchEnvironmentId() throws Exception {
        String response = get(baseUrl + "/api/projects/");
        String searchKey = "\"id\":";
        int idx = response.indexOf(searchKey);
        if (idx == -1) throw new RuntimeException("Could not find project ID in response: " + response.substring(0, Math.min(500, response.length())));
        int start = idx + searchKey.length();
        int end = response.indexOf(",", start);
        return response.substring(start, end).trim();
    }

    private String createDashboard(String name, String description, List<String> tags) throws Exception {
        StringBuilder json = new StringBuilder();
        json.append("{\"name\":\"").append(escapeJson(name)).append("\"");
        if (description != null && !description.isEmpty()) {
            json.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        json.append(",\"pinned\":false");
        if (tags != null && !tags.isEmpty()) {
            json.append(",\"tags\":[");
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJson(tags.get(i))).append("\"");
            }
            json.append("]");
        }
        json.append("}");

        String response = post(baseUrl + "/api/environments/" + environmentId + "/dashboards/", json.toString());
        return extractId(response);
    }

    private void createInsight(String name, String dashboardId, Map<String, Object> def) throws Exception {
        String event = (String) def.get("event");
        String math = (String) def.getOrDefault("math", "total");
        String mathValues = (String) def.get("math_values");
        String display = (String) def.getOrDefault("display", "ActionsLineGraph");
        String interval = (String) def.getOrDefault("interval", "day");
        String dateFrom = (String) def.getOrDefault("date_from", "-30d");
        String breakdown = (String) def.get("breakdown");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"name\":\"").append(escapeJson(name)).append("\"");
        json.append(",\"dashboards\":[").append(dashboardId).append("]");

        // New InsightVizNode + TrendsQuery format
        json.append(",\"query\":{");
        json.append("\"kind\":\"InsightVizNode\",");
        json.append("\"source\":{");
        json.append("\"kind\":\"TrendsQuery\",");

        // Series (EventsNode)
        json.append("\"series\":[{");
        json.append("\"kind\":\"EventsNode\",");
        json.append("\"event\":\"").append(escapeJson(event)).append("\"");
        if (!math.equals("total")) {
            json.append(",\"math\":\"").append(escapeJson(math)).append("\"");
        }
        if (mathValues != null && !mathValues.isEmpty()) {
            json.append(",\"math_property\":\"").append(escapeJson(mathValues)).append("\"");
        }
        json.append("}]"); // close series

        // Date range
        json.append(",\"dateRange\":{\"date_from\":\"").append(escapeJson(dateFrom)).append("\"}");

        // Interval
        json.append(",\"interval\":\"").append(escapeJson(interval)).append("\"");

        // Trends filter (display type)
        json.append(",\"trendsFilter\":{\"display\":\"").append(escapeJson(display)).append("\"}");

        // Breakdown filter
        if (breakdown != null && !breakdown.isEmpty()) {
            json.append(",\"breakdownFilter\":{");
            json.append("\"breakdown_type\":\"event\",");
            json.append("\"breakdown\":\"").append(escapeJson(breakdown)).append("\",");
            json.append("\"breakdown_limit\":10");
            json.append("}");
        }

        json.append("}"); // close source
        json.append("}"); // close query
        json.append("}"); // close root

        post(baseUrl + "/api/environments/" + environmentId + "/insights/", json.toString());
    }

    private List<Map<String, Object>> defineDashboards() {
        List<Map<String, Object>> dashboards = new ArrayList<>();

        dashboards.add(Map.of(
            "name", "LifeMod — Vue Générale",
            "description", "Vue d'ensemble : serveurs actifs, versions, plateformes",
            "tags", List.of("production", "overview"),
            "insights", List.of(
                insight("lifemod_startup", "Serveurs actifs (7j)", Map.of("date_from", "-7d", "math", "unique_user", "display", "ActionsBarValue")),
                insight("lifemod_startup", "Instances actives (30j)", Map.of("date_from", "-30d", "math", "unique_user")),
                insight("lifemod_startup", "Par version LifeMod", Map.of("date_from", "-30d", "breakdown", "plugin_version", "display", "ActionsBarValue")),
                insight("lifemod_startup", "Par plateforme", Map.of("date_from", "-30d", "breakdown", "platform", "display", "ActionsPie")),
                insight("lifemod_startup", "Par type de base de données", Map.of("date_from", "-30d", "breakdown", "database_type", "display", "ActionsPie")),
                insight("lifemod_startup", "Redis activé ?", Map.of("date_from", "-30d", "breakdown", "redis_enabled", "display", "ActionsPie")),
                insight("lifemod_environment", "OS des serveurs", Map.of("date_from", "-30d", "breakdown", "os_name", "display", "ActionsBarValue")),
                insight("lifemod_environment", "Mémoire allouée", Map.of("date_from", "-30d", "display", "ActionsBarValue", "math_values", "allocated_memory_mb"))
            )
        ));

        dashboards.add(Map.of(
            "name", "LifeMod — Configuration",
            "description", "Modules activés/désactivés, réglages",
            "tags", List.of("modules", "config"),
            "insights", List.of(
                insight("lifemod_config_snapshot", "Modules activés (vue d'ensemble)", Map.of("date_from", "-30d", "display", "ActionsTable")),
                insight("lifemod_config_snapshot", "AntiVPN : mode Geo", Map.of("date_from", "-30d", "breakdown", "antivpn_geo_mode", "display", "ActionsPie")),
                insight("lifemod_config_snapshot", "Auto-Punish mode", Map.of("date_from", "-30d", "breakdown", "auto_punish_mode", "display", "ActionsPie")),
                insight("lifemod_config_snapshot", "Commandes activées (nombre)", Map.of("date_from", "-30d", "display", "ActionsBarValue"))
            )
        ));

        dashboards.add(Map.of(
            "name", "LifeMod — Commandes & Usage",
            "description", "Utilisation des commandes, top/flop, heatmap",
            "tags", List.of("commands", "usage"),
            "insights", List.of(
                insight("lifemod_command", "Commandes / jour (7j)", Map.of("date_from", "-7d")),
                insight("lifemod_command", "Commandes / jour (28j)", Map.of("date_from", "-28d", "interval", "week")),
                insight("lifemod_command", "Top 10 commandes", Map.of("date_from", "-30d", "breakdown", "command_name", "display", "ActionsBarValue"))
            )
        ));

        dashboards.add(Map.of(
            "name", "LifeMod — Sanctions & Modération",
            "description", "Sanctions, bans, auto-punish",
            "tags", List.of("sanctions", "moderation"),
            "insights", List.of(
                insight("lifemod_sanction", "Sanctions / jour (7j)", Map.of("date_from", "-7d")),
                insight("lifemod_sanction", "Répartition par type", Map.of("date_from", "-30d", "breakdown", "sanction_type", "display", "ActionsPie")),
                insight("lifemod_sanction", "Bans silencieux vs publics", Map.of("date_from", "-30d", "breakdown", "silent", "display", "ActionsBarValue")),
                insight("lifemod_sanction", "Durées des sanctions", Map.of("date_from", "-30d", "breakdown", "duration_human", "display", "ActionsBarValue")),
                insight("lifemod_sanction", "% sanctions automatiques", Map.of("date_from", "-30d", "breakdown", "auto_punish", "display", "ActionsPie")),
                insight("lifemod_auto_punish", "Auto-punish par catégorie", Map.of("date_from", "-30d", "breakdown", "reason_category", "display", "ActionsBarValue"))
            )
        ));

        dashboards.add(Map.of(
            "name", "LifeMod — Sécurité",
            "description", "AntiVPN, Anti-Alt, Chat filter",
            "tags", List.of("security", "antivpn", "antialt"),
            "insights", List.of(
                insight("lifemod_antivpn_block", "Blocages VPN / jour (7j)", Map.of("date_from", "-7d")),
                insight("lifemod_antivpn_block", "Raisons de blocage", Map.of("date_from", "-30d", "breakdown", "reason", "display", "ActionsPie")),
                insight("lifemod_antialt", "Détections Anti-Alt / jour (7j)", Map.of("date_from", "-7d")),
                insight("lifemod_antialt", "Sévérité des détections", Map.of("date_from", "-30d", "breakdown", "severity", "display", "ActionsPie")),
                insight("lifemod_antialt", "Actions prises", Map.of("date_from", "-30d", "breakdown", "action_taken", "display", "ActionsBarValue"))
            )
        ));

        dashboards.add(Map.of(
            "name", "LifeMod — Staff Tools",
            "description", "Staff mode, freeze, reports",
            "tags", List.of("staff", "modmode", "freeze"),
            "insights", List.of(
                insight("lifemod_staff_mode", "Staff Mode toggles / jour", Map.of("date_from", "-7d")),
                insight("lifemod_freeze", "Freezes / jour (7j)", Map.of("date_from", "-7d")),
                insight("lifemod_report", "Rapports / jour (7j)", Map.of("date_from", "-7d"))
            )
        ));

        dashboards.add(Map.of(
            "name", "LifeMod — Performance & Santé",
            "description", "Erreurs, mémoire, santé du plugin",
            "tags", List.of("performance", "health", "errors"),
            "insights", List.of(
                insight("lifemod_error", "Erreurs / jour (7j)", Map.of("date_from", "-7d")),
                insight("lifemod_error", "Top 10 exceptions", Map.of("date_from", "-30d", "breakdown", "exception_class", "display", "ActionsBarValue")),
                insight("lifemod_error", "Erreurs par contexte", Map.of("date_from", "-30d", "breakdown", "origin", "display", "ActionsBarValue")),
                insight("lifemod_error", "Erreurs par version", Map.of("date_from", "-30d", "breakdown", "plugin_version", "display", "ActionsBarValue")),
                insight("lifemod_environment", "Mémoire max", Map.of("date_from", "-30d", "display", "ActionsBarValue"))
            )
        ));

        return dashboards;
    }

    private Map<String, Object> insight(String event, String name, Map<String, String> overrides) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("event", event);
        m.put("name", name);
        m.putAll(overrides);
        return m;
    }

    private String get(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URI(urlString).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + personalApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int code = conn.getResponseCode();
        String body = readBody(conn);
        if (code >= 400) {
            throw new RuntimeException("GET " + urlString + " returned " + code + ": " + body);
        }
        return body;
    }

    private String post(String urlString, String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URI(urlString).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + personalApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String body = readBody(conn);

        if (code >= 400) {
            if (body.contains("already exists") || body.contains("unique") || body.contains("duplicate")) {
                return null;
            }
            System.err.println("API Error (POST " + urlString + "): " + code);
            System.err.println("Payload: " + json);
            System.err.println("Response: " + body);
            return null;
        }
        return body;
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }

    private String extractId(String response) {
        if (response == null || response.isEmpty()) return null;
        String searchKey = "\"id\":";
        int idx = response.indexOf(searchKey);
        if (idx == -1) return null;
        int start = idx + searchKey.length();
        int end = response.indexOf(",", start);
        if (end == -1) end = response.indexOf("}", start);
        return response.substring(start, end).trim();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
