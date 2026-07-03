package fr.lampalon.lifemod.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Report {
    private int id;
    private UUID reporterUuid;
    private String reporterName;
    private UUID targetUuid;
    private String targetName;
    private String reason;
    private String serverName;
    private String locationWorld;
    private double locationX, locationY, locationZ;
    private ReportStatus status;
    private UUID assignedTo;
    private long createdAt;
    private long updatedAt;
    private long closedAt;
    private String replayId;
    private List<ReportEvidence> evidence;

    public Report() {
        this.status = ReportStatus.OPEN;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.evidence = new ArrayList<>();
    }

    public Report(int id, UUID reporterUuid, String reporterName, UUID targetUuid, String targetName,
                  String reason, String serverName, ReportStatus status, UUID assignedTo,
                  long createdAt, long updatedAt, long closedAt, String replayId) {
        this();
        this.id = id;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reason = reason;
        this.serverName = serverName;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
        this.replayId = replayId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public UUID getReporterUuid() { return reporterUuid; }
    public void setReporterUuid(UUID reporterUuid) { this.reporterUuid = reporterUuid; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public UUID getTargetUuid() { return targetUuid; }
    public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getLocationWorld() { return locationWorld; }
    public void setLocationWorld(String locationWorld) { this.locationWorld = locationWorld; }

    public double getLocationX() { return locationX; }
    public void setLocationX(double x) { this.locationX = x; }

    public double getLocationY() { return locationY; }
    public void setLocationY(double y) { this.locationY = y; }

    public double getLocationZ() { return locationZ; }
    public void setLocationZ(double z) { this.locationZ = z; }

    public void setLocation(String world, double x, double y, double z) {
        this.locationWorld = world;
        this.locationX = x;
        this.locationY = y;
        this.locationZ = z;
    }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getClosedAt() { return closedAt; }
    public void setClosedAt(long closedAt) { this.closedAt = closedAt; }

    public String getReplayId() { return replayId; }
    public void setReplayId(String replayId) { this.replayId = replayId; }

    public List<ReportEvidence> getEvidence() { return evidence; }
    public void setEvidence(List<ReportEvidence> evidence) { this.evidence = evidence; }

    public void addEvidence(ReportEvidence ev) {
        this.evidence.add(ev);
    }
}
