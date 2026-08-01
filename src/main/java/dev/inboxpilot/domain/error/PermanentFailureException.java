package dev.inboxpilot.domain.error;

/**
 * A failure that will never succeed as issued — revoked credentials, a
 * malformed request, a missing mailbox, an unparseable rule.
 *
 * <p>Throw this when retrying would only waste API quota. The message should
 * name what an operator has to change.
 */
public class PermanentFailureException extends InboxPilotException {

    /**
     * @param message what failed and what must change for it to succeed
     */
    public PermanentFailureException(String message) {
        super(Failure.PERMANENT, message);
    }

    /**
     * @param message what failed and what must change for it to succeed
     * @param cause   the underlying provider failure being translated
     */
    public PermanentFailureException(String message, Throwable cause) {
        super(Failure.PERMANENT, message, cause);
    }
}
