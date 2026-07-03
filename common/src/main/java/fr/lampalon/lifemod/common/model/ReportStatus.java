package fr.lampalon.lifemod.common.model;

public enum ReportStatus {
    OPEN("open"),
    ASSIGNED("assigned"),
    CLOSED("closed"),
    REJECTED("rejected");

    private final String configKey;

    ReportStatus(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}
