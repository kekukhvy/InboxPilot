package dev.inboxpilot.domain.error;

/**
 * A failure that may succeed on a later attempt — a rate limit, a timeout, a
 * server-side error, a dropped connection.
 *
 * <p>Throw this when backing off and retrying is the right response. Callers
 * decide how many attempts to make; this type only states that attempting again
 * is worthwhile.
 */
public class TransientFailureException extends InboxPilotException {

    /**
     * @param message what failed and why it may succeed later
     */
    public TransientFailureException(String message) {
        super(Failure.TRANSIENT, message);
    }

    /**
     * @param message what failed and why it may succeed later
     * @param cause   the underlying provider failure being translated
     */
    public TransientFailureException(String message, Throwable cause) {
        super(Failure.TRANSIENT, message, cause);
    }
}
