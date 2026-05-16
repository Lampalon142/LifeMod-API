package fr.lampalon.lifemod.common.service;

import java.util.Optional;
import java.util.UUID;

public interface IPinService {
    /**
     * Stores the initial PIN for a player upon registration.
     * @param uuid The player's UUID.
     * @param pin The generated/set PIN (should be hashed in real scenario).
     */
    void registerPin(UUID uuid, String pin);

    /**
     * Verifies if the provided PIN matches the stored PIN for a player.
     * @param uuid The player's UUID.
     * @param enteredPin The PIN provided by the player.
     * @return true if the PIN matches, false otherwise.
     */
    boolean verifyPin(UUID uuid, String enteredPin);

    /**
     * Resets the PIN for a moderator/admin user (used during recovery).
     * This should only be callable by high-level staff accounts.
     * @param uuid The player's UUID.
     * @param newPin The new PIN to set.
     * @return true if reset was successful.
     */
    boolean resetPin(UUID uuid, String newPin);

    /**
     * Retrieves the current PIN status for auditing purposes (optional).
     * @param uuid The player's UUID.
     * @return Optional containing the PIN hash/status.
     */
    Optional<String> getPinStatus(UUID uuid);
}