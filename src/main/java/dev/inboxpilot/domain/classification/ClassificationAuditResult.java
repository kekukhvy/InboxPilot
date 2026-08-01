package dev.inboxpilot.domain.classification;

/** Outcome recorded for a planned classification change. */
public enum ClassificationAuditResult {
    PLANNED,
    APPLIED,
    FAILED,
    SKIPPED
}
