package fr.lampalon.lifemod.platform.bukkit.managers.staff;

import fr.lampalon.lifemod.platform.bukkit.LifeMod;
import fr.lampalon.lifemod.platform.bukkit.managers.DebugManager;
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
    private final DebugManager debug;

    public StaffActionManager() {
        this.debug = LifeMod.getInstance().getDebugManager();
        registerDefaults();
    }

    private void registerDefaults() {
        registerAction(StaffActionType.JUMP, new JumpAction());
        debug.log("staff", "registerDefaults: JUMP -> JumpAction");
        registerAction(StaffActionType.THRU, new ThruAction());
        debug.log("staff", "registerDefaults: THRU -> ThruAction");
        
        IStaffAction inspector = new InspectorAction();
        registerAction(StaffActionType.INSPECT_INVENTORY, inspector);
        debug.log("staff", "registerDefaults: INSPECT_INVENTORY -> InspectorAction");
        registerAction(StaffActionType.SILENT_CHEST, inspector);
        debug.log("staff", "registerDefaults: SILENT_CHEST -> InspectorAction");
        
        registerAction(StaffActionType.FREEZE, new FreezeAction());
        debug.log("staff", "registerDefaults: FREEZE -> FreezeAction");
        registerAction(StaffActionType.MOUNT, new MountAction());
        debug.log("staff", "registerDefaults: MOUNT -> MountAction");
        registerAction(StaffActionType.RANDOM_TP, new RandomTpAction());
        debug.log("staff", "registerDefaults: RANDOM_TP -> RandomTpAction");
        registerAction(StaffActionType.CPS_TESTER, new CpsAction());
        debug.log("staff", "registerDefaults: CPS_TESTER -> CpsAction");
        registerAction(StaffActionType.KNOCKBACK_TESTER, new KnockbackAction());
        debug.log("staff", "registerDefaults: KNOCKBACK_TESTER -> KnockbackAction");
        registerAction(StaffActionType.VANISH, new VanishAction());
        debug.log("staff", "registerDefaults: VANISH -> VanishAction");
        registerAction(StaffActionType.INFO_VIEWER, new InfoViewerAction());
        debug.log("staff", "registerDefaults: INFO_VIEWER -> InfoViewerAction");
        debug.log("staff", "registerDefaults: total actions registered=" + actions.size());
    }

    public void registerAction(StaffActionType type, IStaffAction action) {
        actions.put(type, action);
    }

    public IStaffAction getAction(StaffActionType type) {
        IStaffAction action = actions.get(type);
        debug.log("staff", "getAction: type=" + type + " found=" + (action != null ? action.getClass().getSimpleName() : "null"));
        return action;
    }
}
