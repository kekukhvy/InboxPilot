package dev.inboxpilot.application.port;

import dev.inboxpilot.application.analysis.AnalysisReport;
import java.nio.file.Path;
import java.util.List;

/** Writes the complete local mailbox-analysis artifact set. */
public interface AnalysisReportStore {

    List<Path> write(AnalysisReport report);
}
