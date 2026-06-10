package fr.lampalon.lifemod.platform.bukkit.managers;

import fr.lampalon.lifemod.common.service.IConfigurationService;

public class ScanConfig {
    private final int maxLocations;
    private final int maxReportLocations;
    private final int timeoutSeconds;
    private final boolean regionFileScan;
    private final int maxRegionFiles;
    private final boolean strictItemMatching;

    public ScanConfig(IConfigurationService config) {
        this.maxLocations = config.getInt("modules.scan.max-locations", 5000);
        this.maxReportLocations = config.getInt("modules.scan.max-report-locations", 10);
        this.timeoutSeconds = config.getInt("modules.scan.timeout-seconds", 120);
        this.regionFileScan = config.getBoolean("modules.scan.region-file-scan", true);
        this.maxRegionFiles = config.getInt("modules.scan.max-region-files", 0);
        this.strictItemMatching = config.getBoolean("modules.scan.strict-item-matching", true);
    }

    public int getMaxLocations() { return maxLocations; }
    public int getMaxReportLocations() { return maxReportLocations; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public boolean isRegionFileScan() { return regionFileScan; }
    public int getMaxRegionFiles() { return maxRegionFiles; }
    public boolean isStrictItemMatching() { return strictItemMatching; }
}
