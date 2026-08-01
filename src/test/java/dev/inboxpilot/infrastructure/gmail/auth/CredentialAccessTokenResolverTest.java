package dev.inboxpilot.infrastructure.gmail.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import dev.inboxpilot.application.model.AccessToken;
import dev.inboxpilot.application.port.CredentialProviderException;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CredentialAccessTokenResolverTest {

    private static final Instant NOW = Instant.parse("2026-08-01T20:27:00Z");
    private static final String ACCESS_TOKEN = "secret-access-token";
    private static final long TOKEN_LIFETIME_SECONDS = 3_600;

    @Mock
    Credential credential;

    private CredentialAccessTokenResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CredentialAccessTokenResolver(
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void keepsAValidCachedAccessTokenWithoutRefreshing() throws Exception {
        usableToken(TOKEN_LIFETIME_SECONDS);

        AccessToken token = resolver.resolve(credential);

        assertThat(token.value()).isEqualTo(ACCESS_TOKEN);
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(TOKEN_LIFETIME_SECONDS));
        verify(credential, never()).refreshToken();
    }

    @Test
    void refreshesAnExpiredAccessTokenBeforeReturningIt() throws Exception {
        when(credential.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(credential.getExpiresInSeconds())
                .thenReturn(-1L)
                .thenReturn(TOKEN_LIFETIME_SECONDS);
        when(credential.refreshToken()).thenReturn(true);

        AccessToken token = resolver.resolve(credential);

        verify(credential).refreshToken();
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(TOKEN_LIFETIME_SECONDS));
    }

    @Test
    void refreshesATokenInsideTheSafetyWindow() throws Exception {
        when(credential.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(credential.getExpiresInSeconds())
                .thenReturn(30L)
                .thenReturn(TOKEN_LIFETIME_SECONDS);
        when(credential.refreshToken()).thenReturn(true);

        resolver.resolve(credential);

        verify(credential).refreshToken();
    }

    @Test
    void refreshesWhenTheCachedAccessTokenIsMissing() throws Exception {
        when(credential.getAccessToken()).thenReturn(null, ACCESS_TOKEN);
        when(credential.getExpiresInSeconds()).thenReturn(TOKEN_LIFETIME_SECONDS);
        when(credential.refreshToken()).thenReturn(true);

        assertThat(resolver.resolve(credential).value()).isEqualTo(ACCESS_TOKEN);

        verify(credential).refreshToken();
    }

    @Test
    void failsClearlyWhenGoogleRejectsTheRefresh() throws Exception {
        when(credential.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(credential.getExpiresInSeconds()).thenReturn(-1L);
        when(credential.refreshToken()).thenReturn(false);

        assertThatExceptionOfType(CredentialProviderException.class)
                .isThrownBy(() -> resolver.resolve(credential))
                .withMessageContaining("refresh");
    }

    @Test
    void rejectsARefreshThatStillProducesNoUsableAccessToken() throws Exception {
        when(credential.getAccessToken()).thenReturn(null);
        when(credential.refreshToken()).thenReturn(true);

        assertThatExceptionOfType(CredentialProviderException.class)
                .isThrownBy(() -> resolver.resolve(credential))
                .withMessageContaining("usable access token");
    }

    private void usableToken(long expiresInSeconds) throws IOException {
        when(credential.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(credential.getExpiresInSeconds()).thenReturn(expiresInSeconds);
    }
}
