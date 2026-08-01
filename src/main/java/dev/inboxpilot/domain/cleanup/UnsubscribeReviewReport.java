package dev.inboxpilot.domain.cleanup;

import java.util.List;

/** Inert unsubscribe mechanisms for manual review. */
public record UnsubscribeReviewReport(List<UnsubscribeReviewEntry> entries) {

    public UnsubscribeReviewReport {
        entries = List.copyOf(entries);
    }
}
