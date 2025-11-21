/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.MeteorClient;

import java.awt.Color;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for sending messages to Discord webhooks.
 */
public class DiscordWebhook {
    private final String webhookUrl;
    private String content;
    private String username;
    private String avatarUrl;
    private final List<Embed> embeds = new ArrayList<>();

    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * Sets the main content/message of the webhook.
     */
    public DiscordWebhook setContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Sets the username that will appear as the sender.
     */
    public DiscordWebhook setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the avatar URL for the webhook sender.
     */
    public DiscordWebhook setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    /**
     * Adds an embed to the webhook message.
     */
    public DiscordWebhook addEmbed(Embed embed) {
        this.embeds.add(embed);
        return this;
    }

    /**
     * Sends the webhook message asynchronously.
     */
    public void send() {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            MeteorClient.LOG.warn("Discord webhook URL is not set");
            return;
        }

        JsonObject payload = new JsonObject();

        if (content != null && !content.isEmpty()) {
            payload.addProperty("content", content);
        }

        if (username != null && !username.isEmpty()) {
            payload.addProperty("username", username);
        }

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            payload.addProperty("avatar_url", avatarUrl);
        }

        if (!embeds.isEmpty()) {
            JsonArray embedsArray = new JsonArray();
            for (Embed embed : embeds) {
                embedsArray.add(embed.toJson());
            }
            payload.add("embeds", embedsArray);
        }

        // Send asynchronously to avoid blocking the game thread
        MeteorExecutor.execute(() -> {
            try {
                HttpResponse<Void> response = Http.post(webhookUrl)
                    .bodyJson(payload.toString())
                    .ignoreExceptions()
                    .sendResponse();

                if (response.statusCode() == 204) {
                    MeteorClient.LOG.debug("Discord webhook sent successfully");
                } else if (response.statusCode() == 429) {
                    MeteorClient.LOG.warn("Discord webhook rate limited");
                } else if (response.statusCode() != 200) {
                    MeteorClient.LOG.warn("Discord webhook failed with status code: {}", response.statusCode());
                }
            } catch (Exception e) {
                MeteorClient.LOG.error("Failed to send Discord webhook", e);
            }
        });
    }

    /**
     * Represents a Discord embed message.
     */
    public static class Embed {
        private String title;
        private String description;
        private String url;
        private Color color;
        private Footer footer;
        private Thumbnail thumbnail;
        private Author author;
        private final List<Field> fields = new ArrayList<>();
        private String timestamp;

        public Embed setTitle(String title) {
            this.title = title;
            return this;
        }

        public Embed setDescription(String description) {
            this.description = description;
            return this;
        }

        public Embed setUrl(String url) {
            this.url = url;
            return this;
        }

        public Embed setColor(Color color) {
            this.color = color;
            return this;
        }

        public Embed setFooter(String text, String iconUrl) {
            this.footer = new Footer(text, iconUrl);
            return this;
        }

        public Embed setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public Embed setAuthor(String name, String url, String iconUrl) {
            this.author = new Author(name, url, iconUrl);
            return this;
        }

        public Embed addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        public Embed setTimestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        JsonObject toJson() {
            JsonObject embed = new JsonObject();

            if (title != null) embed.addProperty("title", title);
            if (description != null) embed.addProperty("description", description);
            if (url != null) embed.addProperty("url", url);
            if (color != null) embed.addProperty("color", color.getRGB() & 0xFFFFFF);
            if (timestamp != null) embed.addProperty("timestamp", timestamp);

            if (footer != null) {
                JsonObject footerObj = new JsonObject();
                footerObj.addProperty("text", footer.text);
                if (footer.iconUrl != null) footerObj.addProperty("icon_url", footer.iconUrl);
                embed.add("footer", footerObj);
            }

            if (thumbnail != null) {
                JsonObject thumbnailObj = new JsonObject();
                thumbnailObj.addProperty("url", thumbnail.url);
                embed.add("thumbnail", thumbnailObj);
            }

            if (author != null) {
                JsonObject authorObj = new JsonObject();
                authorObj.addProperty("name", author.name);
                if (author.url != null) authorObj.addProperty("url", author.url);
                if (author.iconUrl != null) authorObj.addProperty("icon_url", author.iconUrl);
                embed.add("author", authorObj);
            }

            if (!fields.isEmpty()) {
                JsonArray fieldsArray = new JsonArray();
                for (Field field : fields) {
                    JsonObject fieldObj = new JsonObject();
                    fieldObj.addProperty("name", field.name);
                    fieldObj.addProperty("value", field.value);
                    fieldObj.addProperty("inline", field.inline);
                    fieldsArray.add(fieldObj);
                }
                embed.add("fields", fieldsArray);
            }

            return embed;
        }

        private record Footer(String text, String iconUrl) {}
        private record Thumbnail(String url) {}
        private record Author(String name, String url, String iconUrl) {}
        private record Field(String name, String value, boolean inline) {}
    }
}
