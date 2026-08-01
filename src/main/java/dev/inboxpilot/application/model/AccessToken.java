package dev.inboxpilot.application.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A short-lived Gmail access token, authorized ahead of time by
 * {@code CredentialProvider}.
 *
 * <p>Deliberately provider-independent: this is what a Google {@code Credential}
 * looks like once translated to domain terms, so nothing outside
 * {@code dev.inboxpilot.infrastructure.gmail} ever holds a Google client type
 * (ADR 0001, enforced by {@code ArchitectureTest}).
 *
 * @param value     the bearer token to present to the Gmail API
 * @param expiresAt when the token stops being valid
 */
public record AccessToken(String value, Instant expiresAt) {

    /**
     * @throws NullPointerException     if {@code value} or {@code expiresAt} is
     *                                  {@code null}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public AccessToken {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Access token value must not be blank");
        }
    }

    /**
     * Tells whether this token has expired as of the given instant.
     *
     * @param instant the instant to check against
     * @return {@code true} if {@code instant} is at or after {@link #expiresAt}
     */
    public boolean isExpiredAt(Instant instant) {
        return !instant.isBefore(expiresAt);
    }
}
