package dev.inboxpilot.application.classification;

import java.util.List;

/** Complete deterministic dry-run report split into safe review buckets. */
public record ClassificationDryRunReport(
        ClassificationDryRunSummary summary,
        List<ClassificationDryRunEntry> changes,
        List<ClassificationDryRunEntry> conflicts,
        List<ClassificationDryRunEntry> unmatched) {

    public ClassificationDryRunReport {
        changes = List.copyOf(changes);
        conflicts = List.copyOf(conflicts);
        unmatched = List.copyOf(unmatched);
    }
}
