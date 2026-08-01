package dev.inboxpilot.domain.classification;

import java.util.List;

/** Deterministic classification audit entries. */
public record ClassificationAuditReport(List<ClassificationAuditEntry> entries) {

    public ClassificationAuditReport {
        entries = List.copyOf(entries);
    }
}
