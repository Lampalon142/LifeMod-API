package fr.lampalon.lifemod.api;

import fr.lampalon.lifemod.api.antivpn.IVpnService;
import fr.lampalon.lifemod.api.chat.IChatService;
import fr.lampalon.lifemod.api.freeze.IFreezeService;
import fr.lampalon.lifemod.api.player.IPlayerService;
import fr.lampalon.lifemod.api.sanction.ISanctionService;
import fr.lampalon.lifemod.api.staff.IStaffModeService;
import fr.lampalon.lifemod.api.staff.IVanishService;
import fr.lampalon.lifemod.api.webhook.IWebhookService;

/**
 * Public API of the LifeMod plugin. External plugins depending on
 * {@code LifeMod-API.jar} obtain the current instance through
 * {@link Provider#get()} without importing any platform class.
 */
public interface LifeModAPI {

    ISanctionService getSanctionService();

    IVanishService getVanishService();

    IVpnService getVpnService();

    IFreezeService getFreezeService();

    IChatService getChatService();

    IStaffModeService getStaffModeService();

    IWebhookService getWebhookService();

    IPlayerService getPlayerService();

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