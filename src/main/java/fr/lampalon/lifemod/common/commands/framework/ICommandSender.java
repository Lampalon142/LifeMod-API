package fr.lampalon.lifemod.common.commands.framework;

import java.util.UUID;

public interface ICommandSender {
    void sendMessage(String message);
    boolean hasPermission(String permission);
    String getName();
    UUID getUniqueId(); // Sera null pour la console
    boolean isPlayer();
}

