package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.*;

import java.util.EnumMap;
import java.util.Map;

public class StaffActionManager {

    private final Map<StaffActionType, IStaffAction> actions = new EnumMap<>(StaffActionType.class);

    public StaffActionManager() {
        registerDefaults();
    }

    private void registerDefaults() {
        registerAction(StaffActionType.JUMP, new JumpAction());
        registerAction(StaffActionType.THRU, new ThruAction());

        IStaffAction inspector = new InspectorAction();
        registerAction(StaffActionType.INSPECT_INVENTORY, inspector);
        registerAction(StaffActionType.SILENT_CHEST, inspector);

        registerAction(StaffActionType.FREEZE, new FreezeAction());
        registerAction(StaffActionType.MOUNT, new MountAction());
        registerAction(StaffActionType.RANDOM_TP, new RandomTpAction());
        registerAction(StaffActionType.CPS_TESTER, new CPSAction());
        registerAction(StaffActionType.KNOCKBACK_TESTER, new KnockbackAction());
        registerAction(StaffActionType.VANISH, new VanishAction());
        registerAction(StaffActionType.INFO_VIEWER, new InfoViewerAction());
    }

    public void registerAction(StaffActionType type, IStaffAction action) {
        actions.put(type, action);
    }

    public IStaffAction getAction(StaffActionType type) {
        return actions.get(type);
    }
}
