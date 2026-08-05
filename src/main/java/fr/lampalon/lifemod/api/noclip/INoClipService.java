package fr.lampalon.lifemod.api.noclip;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface INoClipService {

    boolean isNoClip(UUID playerUuid);

    void enable(Player player);

    void disable(Player player);

    void toggle(Player player);
}