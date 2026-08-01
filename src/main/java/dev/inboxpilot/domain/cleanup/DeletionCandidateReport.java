package dev.inboxpilot.domain.cleanup;

import java.util.List;

/** Immutable candidates for manual review or a separate future command. */
public record DeletionCandidateReport(List<DeletionCandidate> candidates) {

    public DeletionCandidateReport {
        candidates = List.copyOf(candidates);
    }
}
