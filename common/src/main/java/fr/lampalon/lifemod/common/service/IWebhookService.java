package fr.lampalon.lifemod.common.service;

import fr.lampalon.lifemod.common.model.webhook.WebhookMessage;
import java.util.concurrent.CompletableFuture;

public interface IWebhookService {
    CompletableFuture<Void> send(WebhookMessage message);
    boolean isEnabled();
}
