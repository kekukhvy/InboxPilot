package dev.inboxpilot.domain.analysis;

/** Explainable read-only cleanup candidate classifications. */
public enum CleanupCategory {
    STALE_NEWSLETTER,
    OBSOLETE_ALERT,
    OTP,
    OLD_PROMOTION
}
