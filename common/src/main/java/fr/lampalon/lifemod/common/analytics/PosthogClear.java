package fr.lampalon.lifemod.common.analytics;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PosthogClear {

    private static final String US_API = "https://us.posthog.com";
    private static final String EU_API = "https://eu.posthog.com";

    private final String baseUrl;
    private final String personalApiKey;
    private final String environmentId;
    private boolean dryRun = false;

    public PosthogClear(String region, String personalApiKey, String environmentId) {
        this.baseUrl = region.equalsIgnoreCase("eu") ? EU_API : US_API;
        this.personalApiKey = personalApiKey;
        this.environmentId = environmentId;
    }

    public static void main(String[] args) {
        System.out.println("=== LifeMod PostHog Clear ===");
        System.out.println();

        if (args.length < 2) {
            System.out.println("Usage: java PosthogClear <region> <personal_api_key> [environment_id] [--dry-run]");
            System.out.println();
            System.out.println("  region           : 'us' or 'eu' (your PostHog datacenter)");
            System.out.println("  personal_api_key : generate at https://app.posthog.com/settings/user-api-keys");
            System.out.println("  environment_id   : optional - your project's numeric ID");
            System.out.println("  --dry-run        : list what would be deleted without actually deleting");
            System.out.println();
            System.out.println("Deletes all dashboards whose name starts with \"LifeMod\" and their insights.");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  java PosthogClear us phx_abc123def 12345 --dry-run");
            return;
        }

        String region = args[0];
        String personalApiKey = args[1];
        String environmentId = args.length > 2 && !args[2].equals("--dry-run") ? args[2] : null;
        boolean dryRun = args[args.length - 1].equals("--dry-run");

        PosthogClear clear = new PosthogClear(region, personalApiKey, environmentId);
        clear.dryRun = dryRun;

        try {
            clear.run();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void run() throws Exception {
        String envId = environmentId != null ? environmentId : fetchEnvironmentId();
        System.out.println("Using environment: " + envId);
        System.out.println("API base: " + baseUrl);
        if (dryRun) System.out.println("*** DRY RUN — no changes will be made ***");
        System.out.println();

        // Delete ALL insights in the project
        List<String> allInsightIds = fetchAllInsightIds();
        System.out.println("Found " + allInsightIds.size() + " total insight(s) to delete");
        for (String insightId : allInsightIds) {
            System.out.print("  Deleting insight ID=" + insightId + "... ");
            if (dryRun) {
                System.out.println("SKIP (dry-run)");
            } else {
                deleteInsight(insightId);
                System.out.println("OK");
            }
        }
        System.out.println();

        // Delete all LifeMod dashboards
        List<DashboardInfo> allDashboards = fetchAllDashboards();
        System.out.println("Found " + allDashboards.size() + " total dashboard(s)");

        List<DashboardInfo> lifemodDashboards = new ArrayList<>();
        for (DashboardInfo d : allDashboards) {
            if (d.name != null && d.name.startsWith("LifeMod")) {
                lifemodDashboards.add(d);
            }
        }

        if (lifemodDashboards.isEmpty()) {
            System.out.println("No \"LifeMod\" dashboards found. Nothing to clear.");
        } else {
            System.out.println("Found " + lifemodDashboards.size() + " LifeMod dashboard(s) to delete:");
            for (DashboardInfo d : lifemodDashboards) {
                System.out.println("  \"" + d.name + "\" (ID=" + d.id + ")");
            }
            System.out.println();

            for (DashboardInfo d : lifemodDashboards) {
                System.out.print("Deleting dashboard \"" + d.name + "\" (ID=" + d.id + ")... ");
                if (dryRun) {
                    System.out.println("SKIP (dry-run)");
                } else {
                    deleteDashboard(d.id);
                    System.out.println("OK");
                }
                System.out.println();
            }
        }

        if (dryRun) {
            System.out.println("=== Dry-run complete. Remove --dry-run to actually delete. ===");
        } else {
            System.out.println("=== All insights and LifeMod dashboards cleared! ===");
        }
    }

    private List<String> fetchAllInsightIds() throws Exception {
        List<String> result = new ArrayList<>();
        String url = baseUrl + "/api/environments/" + environmentId + "/insights/?limit=500";
        while (url != null) {
            String response = get(url);
            String searchKey = "\"results\":[";
            int idx = response.indexOf(searchKey);
            if (idx == -1) break;

            int start = idx + searchKey.length();
            int end = response.indexOf("]}", start);
            if (end == -1) end = response.length();

            String json = response.substring(start, end);
            int pos = 0;
            while (pos < json.length()) {
                int idPos = json.indexOf("\"id\":", pos);
                if (idPos == -1) break;

                int objEnd = findMatchingBrace(json, idPos);
                if (objEnd == -1) break;

                String obj = json.substring(idPos, objEnd + 1);
                String id = extractStringValue(obj, "\"id\":", ",");
                if (id != null) {
                    result.add(id);
                }
                pos = objEnd + 1;
            }

            // Handle pagination via "next" link
            url = extractNextUrl(response);
        }
        return result;
    }

    private String extractNextUrl(String response) {
        String key = "\"next\":";
        int idx = response.indexOf(key);
        if (idx == -1) return null;
        int afterValue = idx + key.length();
        // Check for null value
        if (response.substring(afterValue).trim().startsWith("null")) return null;
        int start = response.indexOf("\"", afterValue);
        if (start == -1) return null;
        int end = response.indexOf("\"", start + 1);
        if (end == -1) return null;
        String url = response.substring(start + 1, end);
        return url.isEmpty() ? null : url;
    }

    private String fetchEnvironmentId() throws Exception {
        String response = get(baseUrl + "/api/projects/");
        String searchKey = "\"id\":";
        int idx = response.indexOf(searchKey);
        if (idx == -1) throw new RuntimeException("Could not find project ID in response");
        int start = idx + searchKey.length();
        int end = response.indexOf(",", start);
        return response.substring(start, end).trim();
    }

    private List<DashboardInfo> fetchAllDashboards() throws Exception {
        List<DashboardInfo> result = new ArrayList<>();
        String response = get(baseUrl + "/api/environments/" + environmentId + "/dashboards/?limit=200");
        String searchKey = "\"results\":[";
        int idx = response.indexOf(searchKey);
        if (idx == -1) return result;

        int start = idx + searchKey.length();
        int end = response.lastIndexOf("]}");
        if (end == -1) end = response.length();

        String json = response.substring(start, end);
        int pos = 0;
        while (pos < json.length()) {
            int idPos = json.indexOf("\"id\":", pos);
            if (idPos == -1) break;

            // find the end of this object
            int objEnd = findMatchingBrace(json, idPos);
            if (objEnd == -1) break;

            String obj = json.substring(idPos, objEnd + 1);

            String id = extractStringValue(obj, "\"id\":", ",");
            String name = extractStringValue(obj, "\"name\":\"", "\"");

            if (id != null && name != null) {
                result.add(new DashboardInfo(id, name));
            }

            pos = objEnd + 1;
        }
        return result;
    }

    private void deleteInsight(String insightId) throws Exception {
        patch(baseUrl + "/api/environments/" + environmentId + "/insights/" + insightId + "/", "{\"deleted\":true}");
    }

    private void deleteDashboard(String dashboardId) throws Exception {
        patch(baseUrl + "/api/environments/" + environmentId + "/dashboards/" + dashboardId + "/", "{\"deleted\":true}");
    }

    private void patch(String urlString, String json) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .header("Authorization", "Bearer " + personalApiKey)
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code >= 400 && code != 404) {
            System.err.println("PATCH " + urlString + " returned " + code + ": " + response.body());
        }
    }

    private String get(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
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

    private String readBody(HttpURLConnection conn) throws Exception {
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            java.io.InputStream err = conn.getErrorStream();
            if (err == null) return "";
            try (Scanner scanner = new Scanner(err, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }

    private static String extractStringValue(String json, String key, String delimiter) {
        int keyPos = json.indexOf(key);
        if (keyPos == -1) return null;
        int start = keyPos + key.length();
        int end = json.indexOf(delimiter, start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return null;
        String val = json.substring(start, end).trim();
        // Strip quotes if present
        if (val.startsWith("\"") && val.endsWith("\"")) {
            val = val.substring(1, val.length() - 1);
        }
        if (val.isEmpty()) return null;
        return val;
    }

    private static int findMatchingBrace(String json, int from) {
        int brace = json.indexOf("{", from);
        if (brace == -1) return -1;
        int depth = 0;
        for (int i = brace; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int findMatchingBracket(String json, int from) {
        int bracket = json.indexOf("[", from);
        if (bracket == -1) return -1;
        int depth = 0;
        for (int i = bracket; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static class DashboardInfo {
        final String id;
        final String name;
        DashboardInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
