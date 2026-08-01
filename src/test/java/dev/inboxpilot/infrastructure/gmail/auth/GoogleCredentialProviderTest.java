package dev.inboxpilot.infrastructure.gmail.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.inboxpilot.application.port.CredentialProvider;
import dev.inboxpilot.application.port.CredentialProviderException;
import dev.inboxpilot.infrastructure.config.BatchingProperties;
import dev.inboxpilot.infrastructure.config.AiProperties;
import dev.inboxpilot.infrastructure.config.CheckpointProperties;
import dev.inboxpilot.infrastructure.config.InboxPilotProperties;
import dev.inboxpilot.infrastructure.config.OAuthProperties;
import dev.inboxpilot.infrastructure.config.ReportFormat;
import dev.inboxpilot.infrastructure.config.ReportProperties;
import dev.inboxpilot.infrastructure.config.ScanningProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers AC3 of issue #9 at the port boundary: {@link CredentialProvider}
 * fails with an actionable, permanent error before attempting any browser
 * round-trip when OAuth is not usably configured.
 *
 * <p>The interactive flow itself (issue #9's browser round-trip) cannot run
 * headless in a unit test — every scenario below is reachable without one,
 * because {@link OAuthCredentialGuard} runs first.
 */
@DisplayName("GoogleCredentialProvider")
class GoogleCredentialProviderTest {

    private static final String ENABLED_KEY = "inboxpilot.oauth.enabled";
    private static final String TOKEN_STORE_KEY = "inboxpilot.oauth.token-store";
    private static final String CLIENT_ID = "client-id.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "client-secret";
    private static final List<String> SCOPES =
            List.of("https://www.googleapis.com/auth/gmail.readonly");
    private static final Duration LOOKBACK = Duration.ofDays(1);
    private static final int MAX_MESSAGES = 1;
    private static final int BATCH_SIZE = 1;
    private static final int MAX_RETRIES = 0;
    private static final Duration BACKOFF = Duration.ofSeconds(1);
    private static final int CHECKPOINT_INTERVAL = 1;

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("satisfies the port, so the composition root can wire it")
    void satisfiesThePort() {
        CredentialProvider provider = new GoogleCredentialProvider(
                propertiesWith(new OAuthProperties(false, null, null, tokenStore(), SCOPES)));

        assertThat(provider).isInstanceOf(CredentialProvider.class);
    }

    @Test
    @DisplayName("refuses to authorize when OAuth is disabled, naming the enabling key")
    void refusesWhenDisabled() {
        CredentialProvider provider = new GoogleCredentialProvider(
                propertiesWith(new OAuthProperties(false, null, null, tokenStore(), SCOPES)));

        assertThatExceptionOfType(CredentialProviderException.class)
                .isThrownBy(provider::obtainAccessToken)
                .withMessageContaining(ENABLED_KEY);
    }

    @Test
    @DisplayName("reports the token store failure before attempting the browser flow")
    void reportsTokenStoreFailure() throws IOException {
        Path blockingFile = tempDir.resolve("blocked");
        Files.createFile(blockingFile);
        Path unusableStore = blockingFile.resolve("tokens");
        CredentialProvider provider = new GoogleCredentialProvider(propertiesWith(
                new OAuthProperties(true, CLIENT_ID, CLIENT_SECRET, unusableStore, SCOPES)));

        assertThatExceptionOfType(CredentialProviderException.class)
                .isThrownBy(provider::obtainAccessToken)
                .withMessageContaining(TOKEN_STORE_KEY);
    }

    private Path tokenStore() {
        return tempDir.resolve("tokens");
    }

    private InboxPilotProperties propertiesWith(OAuthProperties oauth) {
        return new InboxPilotProperties(
                oauth,
                new ScanningProperties(LOOKBACK, MAX_MESSAGES, false, false),
                new BatchingProperties(BATCH_SIZE, MAX_RETRIES, BACKOFF, BACKOFF),
                new ReportProperties(tempDir.resolve("reports"), List.of(ReportFormat.JSON), false),
                new CheckpointProperties(false, tempDir.resolve("checkpoint.json"), CHECKPOINT_INTERVAL),
                new AiProperties(false, false, false));
    }
}
