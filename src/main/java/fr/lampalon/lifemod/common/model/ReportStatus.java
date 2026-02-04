package fr.lampalon.lifemod.common.model;

public enum ReportStatus {
    OPEN("open"),
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    CLOSED("closed"),
    REJECTED("rejeted"),
    ARCHIVED("archived");

    private final String configKey;

    ReportStatus(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}


