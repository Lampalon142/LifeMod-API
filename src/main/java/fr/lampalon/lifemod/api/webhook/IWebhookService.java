package fr.lampalon.lifemod.api.webhook;

import java.util.concurrent.CompletableFuture;

public interface IWebhookService {

    CompletableFuture<Void> send(WebhookMessage message);

    boolean isEnabled();
}