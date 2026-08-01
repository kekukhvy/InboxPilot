package dev.inboxpilot.infrastructure.report;

import dev.inboxpilot.domain.cleanup.DeletionCandidate;
import dev.inboxpilot.domain.cleanup.DeletionCandidateReport;

/** Renders review-only deletion candidates as deterministic CSV. */
public final class DeletionCandidateCsvRenderer {

    public String render(DeletionCandidateReport report) {
        StringBuilder csv = new StringBuilder("messageId,category,reason\n");
        report.candidates().forEach(candidate -> csv.append(row(candidate)));
        return csv.toString();
    }

    private static String row(DeletionCandidate candidate) {
        return field(candidate.messageId().value()) + ","
                + field(candidate.category().name()) + ","
                + field(candidate.reason()) + "\n";
    }

    private static String field(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
