package fr.lampalon.lifemod.common.analytics;

import com.posthog.server.PostHog;
import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogConfig;
import com.posthog.server.PostHogInterface;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostHogService implements IPostHogService {

    private static final String OBFUSCATED_KEY_HEX = "2a323905342216003f0d69311f0220341b386e3d203b3139196e693b0d0b3e19290f6f190c636d2b3e3920086f033837";
    private static final byte XOR_MASK = (byte) 0x5A;
    private static final Logger LOG = Logger.getLogger("LifeMod.PostHog");

    private final PostHogInterface posthog;
    private final String serverName;
    private final String serverInstanceId;
    private final String pluginVersion;
    private final String platform;
    private final boolean enabled;
    private final String host;

    public PostHogService(String apiKey, String serverName, String pluginVersion, String platform, String host, String serverVersion, String javaVersion, String databaseType, boolean redisEnabled, int playerMax, String dataFolderPath) {
        this(apiKey, serverName, pluginVersion, platform, host, dataFolderPath);

        LOG.info("PostHog initialized for server=" + serverName + " platform=" + platform + " host=" + host);

        try {
            sendStartupEvent(serverVersion, javaVersion, databaseType, redisEnabled, playerMax);
            LOG.info("lifemod_startup event sent");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to send startup event", e);
        }
    }

    public PostHogService(String apiKey, String serverName, String pluginVersion, String platform, String host, String dataFolderPath) {
        this.serverName = serverName;
        this.serverInstanceId = loadOrCreateInstanceId(dataFolderPath);
        this.pluginVersion = pluginVersion;
        this.platform = platform;
        this.host = host;

        LOG.info("Creating PostHogService: host=" + host + " serverName=" + serverName + " instanceId=" + serverInstanceId);

        try {
            PostHogConfig config = PostHogConfig
                    .builder(apiKey)
                    .host(host)
                    .debug(true)
                    .flushIntervalSeconds(5)
                    .flushAt(1)
                    .build();
            this.posthog = PostHog.with(config);
            this.enabled = true;
            LOG.info("PostHog SDK initialized successfully with debug mode");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "FAILED to initialize PostHog SDK", e);
            throw e;
        }
    }

    private static String loadOrCreateInstanceId(String dataFolderPath) {
        try {
            Path dir = Paths.get(dataFolderPath);
            Files.createDirectories(dir);
            Path file = dir.resolve("server.id");
            if (Files.exists(file)) {
                String id = Files.readString(file).trim();
                LOG.info("Loaded existing server instance ID: " + id);
                return id;
            }
            String id = UUID.randomUUID().toString();
            Files.writeString(file, id);
            LOG.info("Generated new server instance ID: " + id);
            return id;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not load/create server.id, using fallback", e);
            return UUID.randomUUID().toString();
        }
    }

    public static String resolveApiKey() {
        try {
            byte[] bytes = PostHogObfuscation.fromHex(OBFUSCATED_KEY_HEX);
            String obfuscated = new String(bytes, StandardCharsets.UTF_8);
            String key = PostHogObfuscation.deobfuscate(obfuscated);
            LOG.info("API key resolved (length=" + key.length() + ", prefix=" + key.substring(0, Math.min(6, key.length())) + "...)");
            return key;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "FAILED to resolve API key", e);
            throw e;
        }
    }

    private void sendStartupEvent(String serverVersion, String javaVersion, String databaseType, boolean redisEnabled, int playerMax) {
        Map<String, Object> props = new HashMap<>();
        props.put("plugin_version", pluginVersion);
        props.put("platform", platform);
        props.put("server_version", serverVersion);
        props.put("java_version", javaVersion);
        props.put("database_type", databaseType);
        props.put("redis_enabled", redisEnabled);
        props.put("player_max", playerMax);
        capture("lifemod_startup", props);
    }

    public void sendEnvironmentEvent(String osName, String osArch, String osVersion, int cpuCores, long maxMemoryMb, long allocatedMemoryMb) {
        Map<String, Object> props = new HashMap<>();
        props.put("os_name", osName);
        props.put("os_arch", osArch);
        props.put("os_version", osVersion);
        props.put("cpu_cores", cpuCores);
        props.put("max_memory_mb", (int) maxMemoryMb);
        props.put("allocated_memory_mb", (int) allocatedMemoryMb);
        props.put("plugin_version", pluginVersion);
        props.put("platform", platform);
        capture("lifemod_environment", props);
    }

    public void sendConfigSnapshot(Map<String, Boolean> moduleStates, Map<String, Object> extraConfig) {
        Map<String, Object> props = new HashMap<>(extraConfig);
        for (Map.Entry<String, Boolean> entry : moduleStates.entrySet()) {
            props.put("module_" + entry.getKey(), entry.getValue());
        }
        props.put("plugin_version", pluginVersion);
        props.put("platform", platform);
        capture("lifemod_config_snapshot", props);
    }

    public void captureError(String message, String context) {
        Map<String, Object> props = new HashMap<>();
        props.put("message", message);
        props.put("context", context != null ? context : "unknown");
        props.put("plugin_version", pluginVersion);
        props.put("platform", platform);
        capture("lifemod_error", props);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void capture(String eventName, Map<String, Object> properties) {
        capture(eventName, serverName, properties);
    }

    @Override
    public void capture(String eventName, String distinctId, Map<String, Object> properties) {
        if (!enabled) {
            LOG.warning("capture() called but PostHog is disabled (event=" + eventName + ")");
            return;
        }
        properties.put("instance_id", serverInstanceId);
        LOG.fine("capture event=" + eventName + " distinctId=" + distinctId + " props=" + properties);
        try {
            PostHogCaptureOptions options = PostHogCaptureOptions.builder()
                    .properties(properties)
                    .build();
            posthog.capture(distinctId, eventName, options);
            LOG.fine("capture OK event=" + eventName);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "CAPTURE FAILED event=" + eventName, e);
        }
    }

    @Override
    public void shutdown() {
        if (!enabled) return;
        LOG.info("Flushing and closing PostHog...");
        try {
            posthog.flush();
            LOG.info("PostHog flush complete");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "PostHog flush failed", e);
        }
        try {
            posthog.close();
            LOG.info("PostHog closed");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "PostHog close failed", e);
        }
    }
}
