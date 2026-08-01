package dev.inboxpilot.infrastructure.observability;

import dev.inboxpilot.domain.error.Failure;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Attaches operational context — operation name, message id, retry attempt,
 * elapsed time — to every log line emitted while it is open.
 *
 * <p>Putting these fields in the {@linkplain MDC diagnostic context} rather than
 * into each message means a log line is useful even when the code that wrote it
 * knew nothing about the surrounding operation, and it keeps the fields
 * machine-parseable instead of buried in prose. This is what makes the logs
 * answer "which operation, which message, which attempt, how long" (issue #5).
 *
 * <p>Use it as a resource so the context is always restored, including on the
 * failure paths that matter most:
 *
 * <pre>{@code
 * try (OperationContext operation = OperationContext.start("gmail.fetch")) {
 *     operation.withMessageId(id);
 *     operation.withAttempt(attempt);
 *     logger.info("Fetching message");      // carries all of the above
 *     operation.succeeded();
 * }
 * }</pre>
 *
 * <p>Contexts nest. An inner context shadows the keys it sets and puts back
 * whatever the enclosing one held when it closes, so a sub-operation cannot
 * blank out the context of the scan that contains it.
 *
 * <p>Not thread-safe, and deliberately so: an MDC is per-thread, so an instance
 * belongs to the thread that opened it.
 */
public final class OperationContext implements AutoCloseable {

    /** MDC key naming the operation in progress, e.g. {@code gmail.fetch}. */
    public static final String OPERATION_KEY = "operation";

    /** MDC key holding the message being processed, when one applies. */
    public static final String MESSAGE_ID_KEY = "messageId";

    /** MDC key holding the 1-based retry attempt. */
    public static final String ATTEMPT_KEY = "attempt";

    /** MDC key holding elapsed milliseconds, set when the operation ends. */
    public static final String ELAPSED_MILLIS_KEY = "elapsedMs";

    /** MDC key holding the outcome: {@code success} or {@code failure}. */
    public static final String OUTCOME_KEY = "outcome";

    /** MDC key holding the failure classification, when the operation failed. */
    public static final String FAILURE_KEY = "failure";

    static final String OUTCOME_SUCCESS = "success";
    static final String OUTCOME_FAILURE = "failure";

    /** Outcome for an operation that closed without recording success or failure. */
    static final String OUTCOME_UNKNOWN = "unknown";

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationContext.class);

    /** Every key a context owns — the set it snapshots and restores on close. */
    private static final List<String> MANAGED_KEYS = List.of(
            OPERATION_KEY, MESSAGE_ID_KEY, ATTEMPT_KEY,
            ELAPSED_MILLIS_KEY, OUTCOME_KEY, FAILURE_KEY);

    /** Attempt numbering is 1-based: the first try is attempt 1, not a retry. */
    private static final int FIRST_ATTEMPT = 1;

    private final long startedAtNanos;

    /**
     * What the MDC held for {@link #MANAGED_KEYS} before this context opened. A
     * {@code null} value records that the key was absent and must go back to
     * being absent.
     */
    private final Map<String, String> enclosingValues;

    private boolean outcomeRecorded;
    private boolean closed;

    private OperationContext(String operation) {
        this.startedAtNanos = System.nanoTime();
        this.enclosingValues = snapshotEnclosingValues();
        MDC.put(OPERATION_KEY, operation);
    }

    /**
     * Opens a context for the given operation.
     *
     * @param operation dotted operation name, e.g. {@code gmail.fetch}
     * @return the open context; close it to restore the previous state
     * @throws NullPointerException     if {@code operation} is {@code null}
     * @throws IllegalArgumentException if {@code operation} is blank
     */
    public static OperationContext start(String operation) {
        Objects.requireNonNull(operation, "operation");
        if (operation.isBlank()) {
            throw new IllegalArgumentException("Operation name must not be blank");
        }
        return new OperationContext(operation);
    }

    /**
     * Records which message the operation is processing.
     *
     * @param messageId the message identifier; ignored when {@code null}
     * @return this context, for chaining
     */
    public OperationContext withMessageId(String messageId) {
        if (messageId != null) {
            MDC.put(MESSAGE_ID_KEY, messageId);
        }
        return this;
    }

    /**
     * Records which attempt is in progress.
     *
     * @param attempt 1-based attempt number
     * @return this context, for chaining
     * @throws IllegalArgumentException if {@code attempt} is below 1
     */
    public OperationContext withAttempt(int attempt) {
        if (attempt < FIRST_ATTEMPT) {
            throw new IllegalArgumentException(
                    "Attempt must be at least " + FIRST_ATTEMPT + ", but was " + attempt);
        }
        MDC.put(ATTEMPT_KEY, Integer.toString(attempt));
        return this;
    }

    /** Marks the operation successful and records how long it took. */
    public void succeeded() {
        recordOutcome(OUTCOME_SUCCESS, null);
    }

    /**
     * Marks the operation failed and records how long it took.
     *
     * @param failure how the failure should be treated; may be {@code null}
     *                when the classification is unknown
     */
    public void failed(Failure failure) {
        recordOutcome(OUTCOME_FAILURE, failure == null ? null : failure.name());
    }

    /**
     * Returns how long the operation has been running.
     *
     * @return elapsed time since {@link #start(String)}
     */
    public Duration elapsed() {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos);
    }

    private void recordOutcome(String outcome, String failureName) {
        MDC.put(OUTCOME_KEY, outcome);
        MDC.put(ELAPSED_MILLIS_KEY, Long.toString(elapsed().toMillis()));
        if (failureName != null) {
            MDC.put(FAILURE_KEY, failureName);
        }
        outcomeRecorded = true;
    }

    /**
     * Remembers the enclosing context and clears the keys this one has not set
     * yet. Clearing matters: without it a nested operation would inherit the
     * outer operation's message id, attempt, or outcome and every line it logged
     * would name a message it is not processing.
     */
    private static Map<String, String> snapshotEnclosingValues() {
        Map<String, String> snapshot = new HashMap<>();
        for (String key : MANAGED_KEYS) {
            snapshot.put(key, MDC.get(key));
            MDC.remove(key);
        }
        return snapshot;
    }

    /**
     * Puts the enclosing operation's context back, so a nested operation leaves
     * no trace in the one that contains it.
     *
     * <p>An operation that closes without {@link #succeeded()} or
     * {@link #failed(Failure)} — because it threw past them — is logged as it
     * goes, since after this method the context that would explain it is gone.
     *
     * <p>Idempotent: closing twice, as happens when a context is closed
     * explicitly and again by try-with-resources, restores only once.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (!outcomeRecorded) {
            MDC.put(ELAPSED_MILLIS_KEY, Long.toString(elapsed().toMillis()));
            MDC.put(OUTCOME_KEY, OUTCOME_UNKNOWN);
            LOGGER.warn("Operation ended without recording an outcome");
        }
        enclosingValues.forEach((key, value) -> {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
    }
}
