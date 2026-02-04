package fr.lampalon.lifemod.common.messaging;

import java.util.function.Consumer;

public interface IMessagingService {
    /**
     * Publie un message sur un canal spécifique
     */
    void publish(String channel, String message);

    /**
     * S'abonne à un canal
     */
    void subscribe(String channel, Consumer<String> handler);

    /**
     * Ferme la connexion
     */
    void close();
}

