package fr.lampalon.lifemod.api;

import fr.lampalon.lifemod.api.sanction.ISanctionService;
import fr.lampalon.lifemod.api.staff.IVanishService;

public interface LifeModAPI {

    ISanctionService getSanctionService();

    IVanishService getVanishService();
}
