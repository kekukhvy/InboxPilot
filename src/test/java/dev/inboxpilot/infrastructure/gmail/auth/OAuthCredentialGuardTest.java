package dev.inboxpilot.infrastructure.gmail.auth;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import dev.inboxpilot.application.port.CredentialProvider;
import dev.inboxpilot.application.port.CredentialProviderException;
import dev.inboxpilot.infrastructure.config.OAuthProperties;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Covers AC3 of issue #9 — missing or invalid OAuth configuration produces an
 * actionable, permanent failure naming the config key and, where one supplies
 * the value, the environment variable — before any browser flow is attempted.
 *
 * <p>{@link OAuthProperties} already rejects a blank client id or secret at
 * application startup while OAuth is enabled (issue #4), raising
 * {@link IllegalArgumentException}. This guard covers the one gap that check
 * does not: a {@link CredentialProvider} asked to authorize while OAuth is
 * disabled altogether. It also translates {@code OAuthProperties}'
 * {@code IllegalArgumentException} into the port's own exception type, so a
 * caller of {@link CredentialProvider} only ever needs to catch one type.
 */
@DisplayName("OAuthCredentialGuard")
class OAuthCredentialGuardTest {

    private static final String CLIENT_ID = "client-id.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "client-secret";
    private static final Path TOKEN_STORE = Path.of("/tmp/inboxpilot-tokens");
    private static final List<String> SCOPES =
            List.of("https://www.googleapis.com/auth/gmail.readonly");

    private static final String CLIENT_ID_KEY = "inboxpilot.oauth.client-id";
    private static final String CLIENT_SECRET_KEY = "inboxpilot.oauth.client-secret";
    private static final String ENABLED_KEY = "inboxpilot.oauth.enabled";
    private static final String CLIENT_ID_ENV_VAR = "INBOXPILOT_OAUTH_CLIENT_ID";
    private static final String CLIENT_SECRET_ENV_VAR = "INBOXPILOT_OAUTH_CLIENT_SECRET";

    @Nested
    @DisplayName("when OAuth is disabled")
    class WhenDisabled {

        @Test
        @DisplayName("refuses to authorize, naming the enabling key")
        void refusesToAuthorize() {
            OAuthProperties oauth =
                    new OAuthProperties(false, null, null, TOKEN_STORE, SCOPES);

            assertThatExceptionOfType(CredentialProviderException.class)
                    .isThrownBy(() -> OAuthCredentialGuard.requireConfigured(() -> oauth))
                    .withMessageContaining(ENABLED_KEY);
        }
    }

    @Nested
    @DisplayName("when OAuth is enabled")
    class WhenEnabled {

        @Test
        @DisplayName("accepts a fully configured client id and secret")
        void acceptsConfiguredCredentials() {
            OAuthProperties oauth =
                    new OAuthProperties(true, CLIENT_ID, CLIENT_SECRET, TOKEN_STORE, SCOPES);

            assertThatNoException()
                    .isThrownBy(() -> OAuthCredentialGuard.requireConfigured(() -> oauth));
        }

        @Test
        @DisplayName("translates a blank client id into the port's exception type")
        void rejectsBlankClientId() {
            assertThatExceptionOfType(CredentialProviderException.class)
                    .isThrownBy(() -> OAuthCredentialGuard.requireConfigured(
                            () -> new OAuthProperties(true, " ", CLIENT_SECRET, TOKEN_STORE, SCOPES)))
                    .withMessageContaining(CLIENT_ID_KEY)
                    .withMessageContaining(CLIENT_ID_ENV_VAR);
        }

        @Test
        @DisplayName("translates a blank client secret into the port's exception type")
        void rejectsBlankClientSecret() {
            assertThatExceptionOfType(CredentialProviderException.class)
                    .isThrownBy(() -> OAuthCredentialGuard.requireConfigured(
                            () -> new OAuthProperties(true, CLIENT_ID, "", TOKEN_STORE, SCOPES)))
                    .withMessageContaining(CLIENT_SECRET_KEY)
                    .withMessageContaining(CLIENT_SECRET_ENV_VAR);
        }
    }
}
