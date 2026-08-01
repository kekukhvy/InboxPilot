package dev.inboxpilot.application.port;

/**
 * Raised when a {@link MessageSource} cannot read the mailbox.
 *
 * <p>Adapters translate provider-specific failures (transport errors, expired
 * credentials, quota rejections) into this exception, so use cases handle one
 * failure type instead of a provider's error taxonomy.
 */
public class MessageSourceException extends RuntimeException {

    /**
     * @param message what could not be read, and why
     * @param cause   the provider-specific failure being translated
     */
    public MessageSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message what could not be read, and why
     */
    public MessageSourceException(String message) {
        super(message);
    }
}
