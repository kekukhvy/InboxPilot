package dev.inboxpilot.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the shipped {@code application.yml} binds to the typed
 * configuration — the defaults a developer gets on a fresh clone.
 */
@SpringBootTest
@DisplayName("InboxPilot configuration")
class InboxPilotPropertiesTest {

    private static final int EXPECTED_MAX_MESSAGES = 50_000;
    private static final int EXPECTED_BATCH_SIZE = 100;
    private static final int EXPECTED_MAX_RETRIES = 5;
    private static final int EXPECTED_CHECKPOINT_INTERVAL = 500;
    private static final Duration EXPECTED_LOOKBACK = Duration.ofDays(365);
    private static final Duration EXPECTED_INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration EXPECTED_MAX_BACKOFF = Duration.ofSeconds(60);
    private static final String READONLY_SCOPE =
            "https://www.googleapis.com/auth/gmail.readonly";

    @Autowired
    private InboxPilotProperties properties;

    @Test
    @DisplayName("binds every configuration group")
    void bindsEveryGroup() {
        assertThat(properties.oauth()).isNotNull();
        assertThat(properties.scanning()).isNotNull();
        assertThat(properties.batching()).isNotNull();
        assertThat(properties.reports()).isNotNull();
        assertThat(properties.checkpoints()).isNotNull();
    }

    @Test
    @DisplayName("binds OAuth settings with credentials disabled by default")
    void bindsOAuthSettings() {
        OAuthProperties oauth = properties.oauth();

        assertThat(oauth.enabled())
                .as("OAuth stays disabled until credentials are configured (issue #9)")
                .isFalse();
        assertThat(oauth.tokenStore()).isNotNull();
        assertThat(oauth.scopes()).contains(READONLY_SCOPE);
    }

    @Test
    @DisplayName("binds scanning bounds")
    void bindsScanningSettings() {
        ScanningProperties scanning = properties.scanning();

        assertThat(scanning.lookback()).isEqualTo(EXPECTED_LOOKBACK);
        assertThat(scanning.maxMessages()).isEqualTo(EXPECTED_MAX_MESSAGES);
        assertThat(scanning.includeSpam()).isFalse();
        assertThat(scanning.includeTrash()).isFalse();
    }

    @Test
    @DisplayName("binds batching and backoff settings")
    void bindsBatchingSettings() {
        BatchingProperties batching = properties.batching();

        assertThat(batching.batchSize()).isEqualTo(EXPECTED_BATCH_SIZE);
        assertThat(batching.maxRetries()).isEqualTo(EXPECTED_MAX_RETRIES);
        assertThat(batching.initialBackoff()).isEqualTo(EXPECTED_INITIAL_BACKOFF);
        assertThat(batching.maxBackoff()).isEqualTo(EXPECTED_MAX_BACKOFF);
    }

    @Test
    @DisplayName("binds report output settings")
    void bindsReportSettings() {
        ReportProperties reports = properties.reports();

        assertThat(reports.outputDirectory()).isNotNull();
        assertThat(reports.formats())
                .containsExactly(ReportFormat.JSON, ReportFormat.HTML);
        assertThat(reports.overwrite()).isFalse();
    }

    @Test
    @DisplayName("binds checkpoint settings")
    void bindsCheckpointSettings() {
        CheckpointProperties checkpoints = properties.checkpoints();

        assertThat(checkpoints.enabled()).isTrue();
        assertThat(checkpoints.file()).isNotNull();
        assertThat(checkpoints.interval()).isEqualTo(EXPECTED_CHECKPOINT_INTERVAL);
    }

    @Test
    @DisplayName("keeps bound collections immutable")
    void keepsCollectionsImmutable() {
        assertThat(properties.oauth().scopes()).isUnmodifiable();
        assertThat(properties.reports().formats()).isUnmodifiable();
    }

    @Test
    @DisplayName("resolves report and token paths")
    void resolvesPaths() {
        Path tokenStore = properties.oauth().tokenStore();
        Path reportDirectory = properties.reports().outputDirectory();

        assertThat(tokenStore.toString()).isNotBlank();
        assertThat(reportDirectory.toString()).isNotBlank();
    }

    @Test
    @DisplayName("ships a scope list that permits reading mail")
    void shipsReadableScopes() {
        List<String> scopes = properties.oauth().scopes();

        assertThat(scopes).isNotEmpty().allSatisfy(scope -> assertThat(scope).isNotBlank());
    }
}
