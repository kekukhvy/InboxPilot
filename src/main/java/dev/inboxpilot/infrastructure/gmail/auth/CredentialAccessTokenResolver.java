package dev.inboxpilot.infrastructure.gmail.auth;

import com.google.api.client.auth.oauth2.Credential;
import dev.inboxpilot.application.model.AccessToken;
import dev.inboxpilot.application.port.CredentialProviderException;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Refreshes Google credentials before their access token crosses the OAuth boundary. */
final class CredentialAccessTokenResolver {

    private static final long REFRESH_SAFETY_WINDOW_SECONDS = 60;
    private static final long DEFAULT_TOKEN_LIFETIME_SECONDS = 3_600;
    private static final String REFRESH_REJECTED_MESSAGE =
            "Google rejected the cached OAuth refresh token; delete the token store and re-authorize";
    private static final String MISSING_TOKEN_MESSAGE =
            "Google OAuth refresh completed without a usable access token";

    private final Clock clock;

    CredentialAccessTokenResolver(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    AccessToken resolve(Credential credential) throws IOException {
        if (requiresRefresh(credential)) {
            refresh(credential);
        }
        return validatedToken(credential);
    }

    private static boolean requiresRefresh(Credential credential) {
        if (missing(credential.getAccessToken())) {
            return true;
        }
        Long expiresInSeconds = credential.getExpiresInSeconds();
        return expiresInSeconds != null
                && expiresInSeconds <= REFRESH_SAFETY_WINDOW_SECONDS;
    }

    private static void refresh(Credential credential) throws IOException {
        if (!credential.refreshToken()) {
            throw new CredentialProviderException(REFRESH_REJECTED_MESSAGE);
        }
    }

    private AccessToken validatedToken(Credential credential) {
        String value = credential.getAccessToken();
        if (missing(value)) {
            throw new CredentialProviderException(MISSING_TOKEN_MESSAGE);
        }
        long lifetime = tokenLifetime(credential.getExpiresInSeconds());
        Instant expiresAt = clock.instant().plusSeconds(lifetime);
        return new AccessToken(value, expiresAt);
    }

    private static long tokenLifetime(Long expiresInSeconds) {
        if (expiresInSeconds == null) {
            return DEFAULT_TOKEN_LIFETIME_SECONDS;
        }
        if (expiresInSeconds <= 0) {
            throw new CredentialProviderException(MISSING_TOKEN_MESSAGE);
        }
        return expiresInSeconds;
    }

    private static boolean missing(String value) {
        return value == null || value.isBlank();
    }
}
