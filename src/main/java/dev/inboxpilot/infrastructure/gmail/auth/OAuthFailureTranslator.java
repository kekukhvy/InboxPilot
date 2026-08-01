package dev.inboxpilot.infrastructure.gmail.auth;

import dev.inboxpilot.application.port.CredentialProviderException;
import java.io.IOException;
import java.util.Locale;

/**
 * Translates a failure from the Google OAuth client library into
 * {@link CredentialProviderException}, so a caller of
 * {@link dev.inboxpilot.application.port.CredentialProvider} never has to
 * inspect a Google exception to know what to do.
 *
 * <p>Every authorization failure here is permanent: retrying without an
 * operator changing the client credentials or re-authorizing in the browser
 * will not help (project invariant — permanent failures are reported, not
 * retried).
 *
 * <p>Deliberately silent: these methods <em>return</em> an exception rather
 * than throwing it, and the caller that throws it logs it once with the
 * operation context attached. Logging here as well would put every
 * authorization failure in the log two or three times.
 */
final class OAuthFailureTranslator {

    private static final String INVALID_GRANT_MARKER = "invalid_grant";
    private static final String CLIENT_ID_KEY = "inboxpilot.oauth.client-id";
    private static final String CLIENT_SECRET_KEY = "inboxpilot.oauth.client-secret";
    private static final String TOKEN_STORE_KEY = "inboxpilot.oauth.token-store";

    private OAuthFailureTranslator() {
    }

    /**
     * @param cause the failure raised while authorizing or refreshing a token
     * @return a permanent, actionable {@link CredentialProviderException}
     */
    static CredentialProviderException translate(IOException cause) {
        if (isInvalidGrant(cause)) {
            return new CredentialProviderException(
                    "The Gmail refresh token was rejected as invalid or revoked. "
                            + "Delete the cached token and re-authorize, or verify "
                            + CLIENT_ID_KEY + " and " + CLIENT_SECRET_KEY
                            + " still match a Google Cloud OAuth client.", cause);
        }
        return generalFailure(cause);
    }

    /**
     * Translates any checked failure the Google client library can raise while
     * setting up or running the authorization flow — an {@link IOException}
     * during the exchange, or a {@link java.security.GeneralSecurityException}
     * while establishing the HTTPS transport.
     *
     * @param cause the failure raised while authorizing
     * @return a permanent, actionable {@link CredentialProviderException}
     */
    static CredentialProviderException translateIfPossible(Exception cause) {
        if (cause instanceof IOException ioFailure) {
            return translate(ioFailure);
        }
        return generalFailure(cause);
    }

    private static CredentialProviderException generalFailure(Exception cause) {
        return new CredentialProviderException(
                "Gmail authorization failed. Verify " + TOKEN_STORE_KEY
                        + " is reachable and the OAuth client is still valid.", cause);
    }

    private static boolean isInvalidGrant(IOException cause) {
        String message = cause.getMessage();
        return message != null
                && message.toLowerCase(Locale.ROOT).contains(INVALID_GRANT_MARKER);
    }
}
