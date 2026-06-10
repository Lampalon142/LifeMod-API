package fr.lampalon.lifemod.common.model.webhook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebhookMessage {
    private final String content;
    private final String username;
    private final String avatarUrl;
    private final boolean tts;
    private final List<WebhookEmbed> embeds;

    private WebhookMessage(Builder builder) {
        this.content = builder.content;
        this.username = builder.username;
        this.avatarUrl = builder.avatarUrl;
        this.tts = builder.tts;
        this.embeds = builder.embeds.isEmpty()
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(builder.embeds));
    }

    public String getContent() { return content; }
    public String getUsername() { return username; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isTts() { return tts; }
    public List<WebhookEmbed> getEmbeds() { return embeds; }

    public static class Builder {
        private String content;
        private String username;
        private String avatarUrl;
        private boolean tts;
        private final List<WebhookEmbed> embeds = new ArrayList<>();

        public Builder setContent(String content) { this.content = content; return this; }
        public Builder setUsername(String username) { this.username = username; return this; }
        public Builder setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder setTts(boolean tts) { this.tts = tts; return this; }
        public Builder addEmbed(WebhookEmbed embed) { this.embeds.add(embed); return this; }

        public WebhookMessage build() {
            if (content == null && embeds.isEmpty()) {
                throw new IllegalStateException("Set content or add at least one embed");
            }
            return new WebhookMessage(this);
        }
    }
}
