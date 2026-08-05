package fr.lampalon.lifemod.api.report;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IReportService {

    CompletableFuture<Report> createReport(Report report);

    CompletableFuture<Report> getReport(int id);

    CompletableFuture<List<Report>> getReports(int limit);

    CompletableFuture<List<Report>> getReportsByStatus(ReportStatus status, int limit);

    CompletableFuture<Void> updateStatus(int id, ReportStatus status, UUID assignedTo);

    CompletableFuture<Void> setReplayId(int id, String replayId);

    CompletableFuture<Void> addEvidence(ReportEvidence evidence);

    CompletableFuture<List<ReportEvidence>> getEvidence(int reportId);
}