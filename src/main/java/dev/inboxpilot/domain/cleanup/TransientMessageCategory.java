package dev.inboxpilot.domain.cleanup;

/** Explainable categories of short-lived operational mail. */
public enum TransientMessageCategory {
    OTP,
    LOGIN_ALERT,
    DELIVERY_STATUS,
    TRANSIENT_NOTIFICATION
}
