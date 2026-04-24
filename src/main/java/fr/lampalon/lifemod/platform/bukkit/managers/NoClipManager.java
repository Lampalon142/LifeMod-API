package fr.lampalon.lifemod.platform.bukkit.managers;

import com.github.retrooper.packetevents.PacketEvents;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.listeners.NoClipPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

public class NoClipManager {

    private final LifeMod plugin;

    // Joueurs en NoClip + leur GameMode d'origine
    private final Map<UUID, GameMode> noclipPlayers = new HashMap<>();

    public NoClipManager(LifeMod plugin) {
        this.plugin = plugin;
        // Enregistre le listener PacketEvents
        PacketEvents.getAPI().getEventManager()
                .registerListener(new NoClipPacketListener(this));
    }

    // -------------------------------------------------------------------------
    // Toggle
    // -------------------------------------------------------------------------

    public void toggleNoClip(Player player) {
        if (isNoClip(player.getUniqueId())) {
            disableNoClip(player);
        } else {
            enableNoClip(player);
        }
    }

    // -------------------------------------------------------------------------
    // Enable
    // -------------------------------------------------------------------------

    public void enableNoClip(Player player) {
        GameMode original = player.getGameMode();
        noclipPlayers.put(player.getUniqueId(), original);

        // 1. Passe en SPECTATOR côté serveur → bypass natif des collisions/blocs
        player.setGameMode(GameMode.SPECTATOR);

        // 2. Envoie un faux packet Game Event "Change Game Mode" au CLIENT uniquement
        //    avec la valeur du gamemode d'origine → le client affiche toujours le bon HUD
        spoofClientGameMode(player, original);

        player.sendMessage(plugin.getLangConfig().getString(
                "commands.noclip.activate",
                "§aNoClip §7activé — tu traverses les blocs !"));
    }

    // -------------------------------------------------------------------------
    // Disable
    // -------------------------------------------------------------------------

    public void disableNoClip(Player player) {
        GameMode original = noclipPlayers.remove(player.getUniqueId());

        // Rétablit le vrai GameMode serveur
        player.setGameMode(original != null ? original : GameMode.SURVIVAL);

        // Re-synchronise le GameMode côté client (remet le bon HUD)
        spoofClientGameMode(player, original != null ? original : GameMode.SURVIVAL);

        player.sendMessage(plugin.getLangConfig().getString(
                "commands.noclip.deactivate",
                "§cNoClip §7désactivé"));
    }

    // -------------------------------------------------------------------------
    // Spoofing GameMode côté client via PacketEvents
    //
    // Packet Play Server : Game Event (ID 0x20 en 1.20+)
    //   type  = 3  → Change Game Mode
    //   value = 0 Survival / 1 Creative / 2 Adventure / 3 Spectator
    // -------------------------------------------------------------------------

    private void spoofClientGameMode(Player player, GameMode gameMode) {
        int value = switch (gameMode) {
            case SURVIVAL   -> 0;
            case CREATIVE   -> 1;
            case ADVENTURE  -> 2;
            case SPECTATOR  -> 3;
        };

        // WrapperPlayServerGameEvent(type, value)
        // type 3 = "Change Game Mode" dans le protocole Minecraft
        WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(3, value);
        PacketEvents.getAPI().getPlayerManager()
                .sendPacket(player, packet);
    }

    // -------------------------------------------------------------------------
    // Nettoyage shutdown
    // -------------------------------------------------------------------------

    public void shutdown() {
        for (UUID uuid : new HashSet<>(noclipPlayers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) disableNoClip(p);
        }
        noclipPlayers.clear();
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    public boolean isNoClip(UUID uuid) {
        return noclipPlayers.containsKey(uuid);
    }

    public GameMode getOriginalGameMode(UUID uuid) {
        return noclipPlayers.getOrDefault(uuid, GameMode.SURVIVAL);
    }

    public LifeMod getPlugin() {
        return plugin;
    }
}