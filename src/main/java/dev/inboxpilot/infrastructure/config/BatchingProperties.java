package dev.inboxpilot.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * How requests to the mail provider are batched and retried.
 *
 * <p>The Gmail API rate-limits per user, so batch size and retry behaviour are
 * the difference between a scan that completes and one that is throttled into
 * failure. {@code batchSize} is capped at the provider's documented maximum.
 *
 * @param batchSize    messages requested per batch call
 * @param maxRetries   attempts after a retryable failure before giving up
 * @param initialBackoff first wait before retrying; doubles per attempt
 * @param maxBackoff   ceiling on the exponential backoff
 */
public record BatchingProperties(
        @Min(value = MIN_BATCH_SIZE, message = "must request at least {value} message per batch")
        @Max(value = MAX_BATCH_SIZE, message = "must not exceed {value}, the Gmail batch limit")
        int batchSize,
        @Min(value = MIN_RETRIES, message = "must not be negative")
        @Max(value = MAX_RETRIES, message = "must not exceed {value} retries")
        int maxRetries,
        @NotNull(message = "must be set, e.g. 1s")
        Duration initialBackoff,
        @NotNull(message = "must be set, e.g. 60s")
        Duration maxBackoff) {

    static final int MIN_BATCH_SIZE = 1;

    /** Gmail rejects batch requests larger than this. */
    static final int MAX_BATCH_SIZE = 100;

    /** Zero is valid: it means fail fast, without retrying. */
    static final int MIN_RETRIES = 0;

    /** Beyond this, a run stalls rather than failing usefully. */
    static final int MAX_RETRIES = 10;

    /**
     * Rejects a backoff range that can never be honoured. Bean Validation
     * annotations check one field at a time, so this relationship is checked
     * here.
     *
     * @throws IllegalArgumentException if either backoff is negative, or the
     *                                  initial backoff exceeds the maximum
     */
    public BatchingProperties {
        if (initialBackoff != null && initialBackoff.isNegative()) {
            throw new IllegalArgumentException(
                    "inboxpilot.batching.initial-backoff must not be negative, but was "
                            + initialBackoff);
        }
        if (maxBackoff != null && maxBackoff.isNegative()) {
            throw new IllegalArgumentException(
                    "inboxpilot.batching.max-backoff must not be negative, but was " + maxBackoff);
        }
        if (initialBackoff != null && maxBackoff != null && initialBackoff.compareTo(maxBackoff) > 0) {
            throw new IllegalArgumentException(
                    "inboxpilot.batching.initial-backoff (" + initialBackoff
                            + ") must not exceed max-backoff (" + maxBackoff + ")");
        }
    }
}
