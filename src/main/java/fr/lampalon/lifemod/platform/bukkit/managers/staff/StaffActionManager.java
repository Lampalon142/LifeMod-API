package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.IStaffAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.JumpAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.StaffActionType;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.ThruAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.InspectorAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.FreezeAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.MountAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.RandomTpAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.CpsAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.KnockbackAction;
import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.VanishAction;

import java.util.EnumMap;
import java.util.Map;

import fr.lampalon.lifemod.platform.bukkit.managers.staff.action.InfoViewerAction;

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
        registerAction(StaffActionType.CPS_TESTER, new CpsAction());
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
