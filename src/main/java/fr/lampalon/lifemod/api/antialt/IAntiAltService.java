package fr.lampalon.lifemod.api.antialt;

import fr.lampalon.lifemod.api.player.PlayerData;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IAntiAltService {

    CompletableFuture<List<PlayerData>> getAlts(String ip);

    CompletableFuture<AntiAltResult> analyze(UUID playerUuid, String playerName, String ip);
}