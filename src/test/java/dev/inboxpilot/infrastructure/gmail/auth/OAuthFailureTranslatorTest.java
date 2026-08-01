package dev.inboxpilot.infrastructure.gmail.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.port.CredentialProviderException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers AC3 of issue #9 — a revoked or otherwise invalid refresh token
 * produces an actionable, permanent failure rather than an opaque
 * {@link IOException} from the Google client library.
 */
@DisplayName("OAuthFailureTranslator")
class OAuthFailureTranslatorTest {

    private static final String CLIENT_ID_KEY = "inboxpilot.oauth.client-id";
    private static final String TOKEN_STORE_KEY = "inboxpilot.oauth.token-store";

    @Test
    @DisplayName("names the client id key when Google reports invalid_grant")
    void namesTheClientIdKeyOnInvalidGrant() {
        IOException cause = new IOException("Error: invalid_grant, revoked token");

        CredentialProviderException failure = OAuthFailureTranslator.translate(cause);

        assertThat(failure.getMessage()).contains(CLIENT_ID_KEY);
        assertThat(failure.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("points at the token store for any other authorization failure")
    void pointsAtTheTokenStoreForOtherFailures() {
        IOException cause = new IOException("connection reset");

        CredentialProviderException failure = OAuthFailureTranslator.translate(cause);

        assertThat(failure.getMessage()).contains(TOKEN_STORE_KEY);
        assertThat(failure.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("also translates a security failure setting up the HTTPS transport")
    void translatesGeneralSecurityFailures() {
        GeneralSecurityException cause = new GeneralSecurityException("no trusted certificates");

        CredentialProviderException failure = OAuthFailureTranslator.translateIfPossible(cause);

        assertThat(failure.getMessage()).contains(TOKEN_STORE_KEY);
        assertThat(failure.getCause()).isSameAs(cause);
    }
}
