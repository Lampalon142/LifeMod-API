package fr.lampalon.lifemod.api.player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IPlayerService {

    CompletableFuture<PlayerData> getPlayerData(UUID playerUuid);
}