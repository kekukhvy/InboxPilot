package dev.inboxpilot.application.port;

/** Raised when mailbox catalog data cannot be read completely. */
public class MailboxGatewayException extends RuntimeException {

    public MailboxGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public MailboxGatewayException(String message) {
        super(message);
    }
}
