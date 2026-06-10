package fr.lampalon.lifemod.common.model.webhook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebhookEmbed {
    private final String title;
    private final String description;
    private final String url;
    private final int color;
    private final WebhookFooter footer;
    private final WebhookThumbnail thumbnail;
    private final WebhookImage image;
    private final WebhookAuthor author;
    private final List<WebhookField> fields;

    private WebhookEmbed(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.url = builder.url;
        this.color = builder.color;
        this.footer = builder.footer;
        this.thumbnail = builder.thumbnail;
        this.image = builder.image;
        this.author = builder.author;
        this.fields = builder.fields.isEmpty()
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(builder.fields));
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public int getColor() { return color; }
    public WebhookFooter getFooter() { return footer; }
    public WebhookThumbnail getThumbnail() { return thumbnail; }
    public WebhookImage getImage() { return image; }
    public WebhookAuthor getAuthor() { return author; }
    public List<WebhookField> getFields() { return fields; }

    public static class Builder {
        private String title;
        private String description;
        private String url;
        private int color;
        private WebhookFooter footer;
        private WebhookThumbnail thumbnail;
        private WebhookImage image;
        private WebhookAuthor author;
        private final List<WebhookField> fields = new ArrayList<>();

        public Builder setTitle(String title) { this.title = title; return this; }
        public Builder setDescription(String description) { this.description = description; return this; }
        public Builder setUrl(String url) { this.url = url; return this; }
        public Builder setColor(int color) { this.color = color; return this; }
        public Builder setFooter(WebhookFooter footer) { this.footer = footer; return this; }
        public Builder setThumbnail(WebhookThumbnail thumbnail) { this.thumbnail = thumbnail; return this; }
        public Builder setImage(WebhookImage image) { this.image = image; return this; }
        public Builder setAuthor(WebhookAuthor author) { this.author = author; return this; }
        public Builder addField(WebhookField field) { this.fields.add(field); return this; }

        public WebhookEmbed build() {
            return new WebhookEmbed(this);
        }
    }
}
