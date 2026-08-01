package dev.inboxpilot.application.port;

/**
 * Raised when a {@link CredentialProvider} cannot authorize access to the
 * mailbox.
 *
 * <p>Adapters translate provider-specific failures — a blank client id or
 * secret, a token store that cannot be created, a revoked or invalid refresh
 * token — into this exception, naming the offending configuration key and the
 * environment variable that supplies it wherever one applies. These are
 * permanent failures: retrying without the operator changing something will
 * not help.
 */
public class CredentialProviderException extends RuntimeException {

    /**
     * @param message what could not be authorized, and what an operator must
     *                change for it to succeed
     * @param cause   the underlying provider failure being translated
     */
    public CredentialProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message what could not be authorized, and what an operator must
     *                change for it to succeed
     */
    public CredentialProviderException(String message) {
        super(message);
    }
}
