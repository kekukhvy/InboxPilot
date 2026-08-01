package dev.inboxpilot.domain.analysis;

import java.util.List;

/** Immutable cleanup candidates for human review. */
public record CleanupCandidateReport(List<CleanupCandidate> candidates) {

    public CleanupCandidateReport {
        candidates = List.copyOf(candidates);
    }
}
