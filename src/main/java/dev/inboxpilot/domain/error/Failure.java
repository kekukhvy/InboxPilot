package dev.inboxpilot.domain.error;

/**
 * Whether a failure is worth retrying.
 *
 * <p>This is the single most important question about any error InboxPilot
 * hits: a rate-limit response should back off and retry, while a revoked token
 * or a malformed rule never will succeed no matter how many attempts follow.
 * Retrying a permanent failure wastes API quota; giving up on a transient one
 * loses a scan that would have completed.
 *
 * <p>Deliberately a domain concept, not an HTTP-status detail — adapters map
 * their provider's error taxonomy onto this before it travels inward.
 */
public enum Failure {

    /**
     * The operation may succeed if attempted again — a rate limit, a timeout, a
     * server-side error, a dropped connection. Callers should retry with
     * backoff.
     */
    TRANSIENT,

    /**
     * The operation will never succeed as issued — invalid credentials, a
     * malformed request, a missing mailbox, a rule that cannot be parsed.
     * Callers should stop and report.
     */
    PERMANENT;

    /**
     * Tells whether a caller should retry an operation that failed this way.
     *
     * @return {@code true} for {@link #TRANSIENT}
     */
    public boolean isRetryable() {
        return this == TRANSIENT;
    }
}
