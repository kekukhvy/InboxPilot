package dev.inboxpilot.domain.error;

import java.util.Objects;

/**
 * Base type for every failure InboxPilot raises deliberately.
 *
 * <p>Carries the one fact a caller needs in order to react: whether the failure
 * is {@link Failure#TRANSIENT} and worth retrying, or {@link Failure#PERMANENT}
 * and worth reporting. Code handling errors branches on
 * {@link #isRetryable()} rather than on the exception's concrete class, so a new
 * failure type does not require touching every retry site.
 *
 * <p>Unchecked, because these failures are not something a caller can meaningfully
 * recover from at every call site; they are handled where retry or reporting
 * policy lives. Plain Java with no framework imports — this is domain code.
 */
public abstract class InboxPilotException extends RuntimeException {

    private final transient Failure failure;

    /**
     * @param failure whether the failure is transient or permanent
     * @param message what failed, in terms an operator can act on
     * @throws NullPointerException if {@code failure} is {@code null}
     */
    protected InboxPilotException(Failure failure, String message) {
        super(message);
        this.failure = Objects.requireNonNull(failure, "failure");
    }

    /**
     * @param failure whether the failure is transient or permanent
     * @param message what failed, in terms an operator can act on
     * @param cause   the underlying failure being translated
     * @throws NullPointerException if {@code failure} is {@code null}
     */
    protected InboxPilotException(Failure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = Objects.requireNonNull(failure, "failure");
    }

    /**
     * Returns how this failure should be treated.
     *
     * @return the failure classification, never {@code null}
     */
    public Failure failure() {
        return failure;
    }

    /**
     * Tells whether retrying the operation could succeed.
     *
     * @return {@code true} when the failure is transient
     */
    public boolean isRetryable() {
        return failure.isRetryable();
    }
}
