package dev.inboxpilot.domain.classification;

import java.util.List;

/** Complete post-execution verification scan result. */
public record ClassificationVerificationReport(List<ClassificationVerificationEntry> entries) {

    public ClassificationVerificationReport {
        entries = List.copyOf(entries);
    }

    public boolean successful() {
        return entries.stream()
                .allMatch(entry -> entry.status() == ClassificationVerificationStatus.MATCH);
    }
}
