package fr.lampalon.lifemod.common.service;

import java.util.Optional;
import java.util.UUID;

public class PinServiceImpl implements IPinService {
    // Placeholder for Database interaction (e.g., calling a DAO/Repository)
    // In a real scenario, this would interact with the database layer responsible for user credentials.

    @Override
    public void registerPin(UUID uuid, String pin) {
        System.out.println("[PINService] Storing PIN for " + uuid + ": " + pin);
        // TODO: Implement actual hashing and DB save here. Use BCrypt or similar secure method.
    }

    @Override
    public boolean verifyPin(UUID uuid, String enteredPin) {
        System.out.println("[PINService] Verifying PIN for " + uuid + ". Entered: " + enteredPin);
        // TODO: Retrieve hashed pin from DB and compare with enteredPin using the same hashing mechanism.
        return true; // Placeholder success
    }

    @Override
    public boolean resetPin(UUID uuid, String newPin) {
        System.out.println("[PINService] Resetting PIN for " + uuid + ". New PIN set.");
        registerPin(uuid, newPin); // Re-using registration logic after successful admin override
        return true;
    }

    @Override
    public Optional<String> getPinStatus(UUID uuid) {
        // TODO: Implement DB lookup for pin status.
        return Optional.empty();
    }
}