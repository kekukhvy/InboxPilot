package dev.inboxpilot.domain.cleanup;

/** Review-only reasons a message may be safe to delete in a future command. */
public enum DeletionCandidateCategory {
    STALE_NEWSLETTER,
    OTP,
    LOGIN_ALERT,
    DELIVERY_STATUS,
    TRANSIENT_NOTIFICATION
}
