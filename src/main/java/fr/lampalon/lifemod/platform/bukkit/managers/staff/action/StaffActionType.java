package fr.lampalon.lifemod.platform.bukkit.managers.staff.action;

public enum StaffActionType {
    THRU(false),
    JUMP(false),
    INSPECT_INVENTORY(true),
    SILENT_CHEST(true),
    FREEZE(true),
    MOUNT(true),
    RANDOM_TP(false),
    CPS_TESTER(true),
    KNOCKBACK_TESTER(true),
    INFO_VIEWER(true),
    VANISH(false),
    COMMAND(false);

    private final boolean requiresTarget;

    StaffActionType(boolean requiresTarget) {
        this.requiresTarget = requiresTarget;
    }

    public boolean requiresTarget() {
        return requiresTarget;
    }

    public static StaffActionType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
