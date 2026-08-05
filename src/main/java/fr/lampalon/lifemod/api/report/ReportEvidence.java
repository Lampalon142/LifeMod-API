package fr.lampalon.lifemod.api.report;

import java.util.UUID;

public class ReportEvidence {

    private int id;
    private int reportId;
    private String type;
    private String data;
    private UUID authorUuid;
    private String authorName;
    private long createdAt;

    public ReportEvidence() {
        this.createdAt = System.currentTimeMillis();
    }

    public ReportEvidence(int id, int reportId, String type, String data,
                          UUID authorUuid, String authorName, long createdAt) {
        this();
        this.id = id;
        this.reportId = reportId;
        this.type = type;
        this.data = data;
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public UUID getAuthorUuid() { return authorUuid; }
    public void setAuthorUuid(UUID authorUuid) { this.authorUuid = authorUuid; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}