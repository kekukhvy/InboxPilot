package dev.inboxpilot.application.classification;

import java.nio.file.Path;
import java.util.List;

/** CLI-facing result of a read-only classification simulation. */
public record ClassificationDryRunResult(
        ClassificationDryRunSummary summary, List<Path> reports) {

    public ClassificationDryRunResult {
        reports = List.copyOf(reports);
    }
}
