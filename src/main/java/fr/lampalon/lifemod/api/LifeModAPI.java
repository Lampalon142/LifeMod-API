package fr.lampalon.lifemod.api;

import fr.lampalon.lifemod.api.sanction.ISanctionService;
import fr.lampalon.lifemod.api.staff.IVanishService;

/**
 * Public API of the LifeMod plugin. External plugins depending on
 * {@code LifeMod-API.jar} obtain the current instance through
 * {@link Provider#get()} without importing any platform class.
 */
public interface LifeModAPI {

    ISanctionService getSanctionService();

    IVanishService getVanishService();

    /**
     * Static holder for the currently running {@link LifeModAPI} instance,
     * populated by LifeMod on startup.
     */
    final class Provider {

        private static LifeModAPI instance;

        private Provider() {
        }

        public static void set(LifeModAPI api) {
            instance = api;
        }

        public static LifeModAPI get() {
            return instance;
        }
    }
}