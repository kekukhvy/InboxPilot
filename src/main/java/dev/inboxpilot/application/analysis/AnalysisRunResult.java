package dev.inboxpilot.application.analysis;

import java.nio.file.Path;
import java.util.List;

/** Summary of the read-only mailbox analysis. */
public record AnalysisRunResult(
        int processedMessages,
        int unclassifiedSenders,
        int unclassifiedDomains,
        List<Path> reports) {

    public AnalysisRunResult {
        reports = List.copyOf(reports);
    }
}
