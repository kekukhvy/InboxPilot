package dev.inboxpilot.infrastructure.gmail.auth;

import dev.inboxpilot.application.port.CredentialProvider;
import dev.inboxpilot.application.port.CredentialProviderException;
import dev.inboxpilot.infrastructure.config.OAuthProperties;
import java.util.function.Supplier;

/**
 * Guards a {@link CredentialProvider} against authorizing with missing or
 * unconfigured OAuth settings, before any browser round-trip is attempted.
 *
 * <p>Takes a {@link Supplier} rather than an already-built
 * {@link OAuthProperties} so that {@code OAuthProperties}' own validation —
 * it refuses to construct with a blank client id or secret while enabled
 * (issue #4) — runs inside this guard and its {@link IllegalArgumentException}
 * is translated into {@link CredentialProviderException}. Callers of
 * {@link CredentialProvider} then only ever need to catch one exception type.
 */
final class OAuthCredentialGuard {

    private static final String ENABLED_KEY = "inboxpilot.oauth.enabled";
    private static final String CLIENT_ID_KEY = "inboxpilot.oauth.client-id";
    private static final String CLIENT_SECRET_KEY = "inboxpilot.oauth.client-secret";

    private OAuthCredentialGuard() {
    }

    /**
     * @param oauth supplies the bound OAuth configuration, validating it on
     *              access
     * @return the validated configuration, enabled and fully credentialed
     * @throws CredentialProviderException if OAuth is disabled, or the client
     *                                      id or secret is blank
     */
    static OAuthProperties requireConfigured(Supplier<OAuthProperties> oauth) {
        OAuthProperties properties = resolve(oauth);
        if (!properties.enabled()) {
            throw new CredentialProviderException(
                    ENABLED_KEY + " is false. Set it to true and configure "
                            + CLIENT_ID_KEY + " and " + CLIENT_SECRET_KEY
                            + " before requesting Gmail access.");
        }
        return properties;
    }

    private static OAuthProperties resolve(Supplier<OAuthProperties> oauth) {
        try {
            return oauth.get();
        } catch (IllegalArgumentException e) {
            throw new CredentialProviderException(e.getMessage(), e);
        }
    }
}
