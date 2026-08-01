package dev.inboxpilot.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;

/**
 * Google OAuth settings for desktop authorization.
 *
 * <p><strong>Secrets never belong in committed YAML.</strong>
 * {@code clientSecret} is supplied through the environment; the committed
 * {@code application-example.yml} carries placeholders only. See the
 * Configuration section of the README.
 *
 * <p>Credentials are validated only when {@code enabled} is {@code true}. That
 * keeps the application runnable before Gmail access has been set up (issue #9)
 * while still failing loudly the moment someone enables it with a blank client
 * id — the validation messages are spelled out in English so the failure reads
 * the same on every machine, regardless of the JVM's default locale.
 *
 * @param enabled      whether Gmail access is configured and should be validated
 * @param clientId     OAuth client id issued by Google Cloud
 * @param clientSecret OAuth client secret; supply via environment variable
 * @param tokenStore   directory holding the refresh token, kept outside the repo
 * @param scopes       Gmail scopes requested at authorization; at least one
 */
public record OAuthProperties(
        boolean enabled,
        String clientId,
        String clientSecret,
        @NotNull(message = "must be set: the directory that will hold the OAuth refresh token")
        Path tokenStore,
        @NotEmpty(message = "must list at least one Gmail scope")
        List<@NotBlank(message = "must not be blank") String> scopes) {

    private static final String CLIENT_ID_KEY = "inboxpilot.oauth.client-id";
    private static final String CLIENT_SECRET_KEY = "inboxpilot.oauth.client-secret";
    private static final String ENABLED_KEY = "inboxpilot.oauth.enabled";

    /**
     * @throws IllegalArgumentException if OAuth is enabled but a credential is
     *                                  missing, naming the environment variable
     *                                  that supplies it
     */
    public OAuthProperties {
        if (enabled) {
            requireCredential(clientId, CLIENT_ID_KEY, "INBOXPILOT_OAUTH_CLIENT_ID");
            requireCredential(clientSecret, CLIENT_SECRET_KEY, "INBOXPILOT_OAUTH_CLIENT_SECRET");
        }
        if (scopes != null) {
            scopes = List.copyOf(scopes);
        }
    }

    private static void requireCredential(String value, String key, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    key + " must not be blank when " + ENABLED_KEY + " is true. "
                            + "Set the " + environmentVariable + " environment variable "
                            + "(never commit it), or set " + ENABLED_KEY + " to false "
                            + "until Gmail access is configured.");
        }
    }
}
