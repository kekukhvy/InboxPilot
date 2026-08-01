package dev.inboxpilot.domain.classification;

/** Post-execution comparison outcome for one planned message. */
public enum ClassificationVerificationStatus {
    MATCH,
    LABEL_MISMATCH,
    MESSAGE_MISSING
}
