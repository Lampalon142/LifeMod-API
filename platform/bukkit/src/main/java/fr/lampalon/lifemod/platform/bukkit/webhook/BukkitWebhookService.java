package fr.lampalon.lifemod.platform.bukkit.webhook;

import com.google.gson.Gson;
import fr.lampalon.lifemod.common.model.webhook.*;
import fr.lampalon.lifemod.common.service.IWebhookService;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BukkitWebhookService implements IWebhookService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String USER_AGENT = "LifeMod/2.0";

    private final OkHttpClient client;
    private final String webhookUrl;
    private final Gson gson;
    private final Logger logger;
    private final boolean configEnabled;

    public BukkitWebhookService(String webhookUrl, boolean configEnabled, Logger logger) {
        this.webhookUrl = webhookUrl;
        this.configEnabled = configEnabled;
        this.logger = logger;
        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        this.gson = new Gson();

        if (!configEnabled) {
            logger.info("[Webhook] modules.discord.enabled = false. Set to true in config.yml to activate Discord alerts.");
        } else if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warning("[Webhook] modules.discord.enabled = true but no webhook URL configured (null/empty). Discord alerts will be disabled.");
        } else if ("INSERT_YOUR_WEBHOOK_HERE".equals(webhookUrl)) {
            logger.warning("[Webhook] modules.discord.enabled = true but webhook URL is still the default placeholder. Discord alerts will be disabled.");
        } else {
            logger.info("[Webhook] Webhook URL configured. Discord alerts are enabled.");
        }
    }

    @Override
    public boolean isEnabled() {
        if (!configEnabled) return false;
        boolean urlValid = webhookUrl != null && !webhookUrl.isEmpty()
                && !"INSERT_YOUR_WEBHOOK_HERE".equals(webhookUrl);
        if (!urlValid) {
            logger.fine("[Webhook] isEnabled() = false — webhook URL is missing or still the placeholder.");
        }
        return urlValid;
    }

    @Override
    public CompletableFuture<Void> send(WebhookMessage message) {
        String json = serialize(message);
        logger.fine("[Webhook] Sending POST to Discord webhook. Payload: " + json);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .addHeader("User-Agent", USER_AGENT)
                .build();

        CompletableFuture<Void> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.warning("[Webhook] Request failed: " + e.getMessage());
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        logger.fine("[Webhook] Sent successfully (HTTP " + response.code() + ")");
                        future.complete(null);
                    } else {
                        String body = response.body() != null ? response.body().string() : "(no body)";
                        logger.warning("[Webhook] Discord responded with HTTP " + response.code() + ": " + body);
                        future.completeExceptionally(
                                new IOException("Discord responded with " + response.code() + ": " + body));
                    }
                } catch (IOException e) {
                    logger.warning("[Webhook] Failed to read response body: " + e.getMessage());
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    private String serialize(WebhookMessage message) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (message.getContent() != null) root.put("content", message.getContent());
        if (message.getUsername() != null) root.put("username", message.getUsername());
        if (message.getAvatarUrl() != null) root.put("avatar_url", message.getAvatarUrl());
        root.put("tts", message.isTts());

        List<WebhookEmbed> embeds = message.getEmbeds();
        if (!embeds.isEmpty()) {
            List<Map<String, Object>> embedsList = new ArrayList<>(embeds.size());
            for (WebhookEmbed embed : embeds) {
                embedsList.add(serializeEmbed(embed));
            }
            root.put("embeds", embedsList);
        }

        return gson.toJson(root);
    }

    private Map<String, Object> serializeEmbed(WebhookEmbed embed) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (embed.getTitle() != null) map.put("title", embed.getTitle());
        if (embed.getDescription() != null) map.put("description", embed.getDescription());
        if (embed.getUrl() != null) map.put("url", embed.getUrl());
        map.put("color", embed.getColor());

        WebhookFooter footer = embed.getFooter();
        if (footer != null) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("text", footer.text());
            if (footer.iconUrl() != null) f.put("icon_url", footer.iconUrl());
            map.put("footer", f);
        }

        WebhookImage image = embed.getImage();
        if (image != null) {
            map.put("image", Collections.singletonMap("url", image.url()));
        }

        WebhookThumbnail thumbnail = embed.getThumbnail();
        if (thumbnail != null) {
            map.put("thumbnail", Collections.singletonMap("url", thumbnail.url()));
        }

        WebhookAuthor author = embed.getAuthor();
        if (author != null) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("name", author.name());
            if (author.url() != null) a.put("url", author.url());
            if (author.iconUrl() != null) a.put("icon_url", author.iconUrl());
            map.put("author", a);
        }

        List<WebhookField> fields = embed.getFields();
        if (!fields.isEmpty()) {
            List<Map<String, Object>> fieldsList = new ArrayList<>(fields.size());
            for (WebhookField field : fields) {
                fieldsList.add(Map.of("name", field.name(), "value", field.value(), "inline", field.inline()));
            }
            map.put("fields", fieldsList);
        }

        return map;
    }
}
