package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

public enum StaffActionType {
    // Navigation
    THRU,
    JUMP,
    
    // Inspection
    INSPECT_INVENTORY,
    SILENT_CHEST,
    
    // Control
    FREEZE,
    MOUNT,
    RANDOM_TP,
    
    // Analysis
    CPS_TESTER,
    KNOCKBACK_TESTER,
    INFO_VIEWER,
    
    // Utility
    VANISH,
    COMMAND; 
    
    public static StaffActionType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
