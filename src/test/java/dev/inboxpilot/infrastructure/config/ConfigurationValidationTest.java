package dev.inboxpilot.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.inboxpilot.InboxPilotApplication;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Proves the "startup fails clearly on invalid configuration" acceptance
 * criterion of issue #4.
 *
 * <p>Each test starts a real application context with one setting broken and
 * asserts that startup fails <em>and</em> that the failure names the offending
 * key. A validation rule that merely exists is worthless — these tests fail if
 * a bad value is ever silently accepted.
 */
@DisplayName("Configuration validation")
class ConfigurationValidationTest {

    private static final String MAX_MESSAGES_KEY = "inboxpilot.scanning.max-messages";
    private static final String BATCH_SIZE_KEY = "inboxpilot.batching.batch-size";
    private static final String MAX_RETRIES_KEY = "inboxpilot.batching.max-retries";
    private static final String LOOKBACK_KEY = "inboxpilot.scanning.lookback";
    private static final String FORMATS_KEY = "inboxpilot.reports.formats";
    private static final String CLIENT_ID_KEY = "inboxpilot.oauth.client-id";
    private static final String INTERVAL_KEY = "inboxpilot.checkpoints.interval";

    @Test
    @DisplayName("accepts the configuration the project ships with")
    void acceptsShippedConfiguration() {
        try (ConfigurableApplicationContext context = startWith()) {
            assertThat(context.isRunning()).isTrue();
        }
    }

    @Nested
    @DisplayName("rejects out-of-range values")
    class RejectsOutOfRangeValues {

        @Test
        @DisplayName("a scan of zero messages")
        void rejectsZeroMaxMessages() {
            assertStartupFailsNaming(MAX_MESSAGES_KEY, MAX_MESSAGES_KEY + "=0");
        }

        @Test
        @DisplayName("a batch larger than the Gmail limit")
        void rejectsOversizedBatch() {
            assertStartupFailsNaming(BATCH_SIZE_KEY, BATCH_SIZE_KEY + "=101");
        }

        @Test
        @DisplayName("a negative retry count")
        void rejectsNegativeRetries() {
            assertStartupFailsNaming(MAX_RETRIES_KEY, MAX_RETRIES_KEY + "=-1");
        }

        @Test
        @DisplayName("a checkpoint interval of zero")
        void rejectsZeroCheckpointInterval() {
            assertStartupFailsNaming(INTERVAL_KEY, INTERVAL_KEY + "=0");
        }
    }

    @Nested
    @DisplayName("rejects malformed values")
    class RejectsMalformedValues {

        @Test
        @DisplayName("a lookback that is not a duration")
        void rejectsUnparseableDuration() {
            assertStartupFailsNaming(LOOKBACK_KEY, LOOKBACK_KEY + "=not-a-duration");
        }

        @Test
        @DisplayName("an unknown report format")
        void rejectsUnknownReportFormat() {
            assertStartupFailsNaming(FORMATS_KEY, FORMATS_KEY + "[0]=PDF");
        }

        @Test
        @DisplayName("an empty report format list")
        void rejectsEmptyReportFormats() {
            assertStartupFailsNaming(FORMATS_KEY, FORMATS_KEY + "=");
        }
    }

    @Nested
    @DisplayName("rejects contradictory values")
    class RejectsContradictoryValues {

        @Test
        @DisplayName("an initial backoff larger than the maximum")
        void rejectsInvertedBackoffRange() {
            assertStartupFailsNaming("max-backoff",
                    "inboxpilot.batching.initial-backoff=60s",
                    "inboxpilot.batching.max-backoff=1s");
        }
    }

    @Nested
    @DisplayName("OAuth credentials")
    class OAuthCredentials {

        @Test
        @DisplayName("are required once OAuth is enabled")
        void requiresCredentialsWhenEnabled() {
            assertStartupFailsNaming(CLIENT_ID_KEY,
                    "inboxpilot.oauth.enabled=true",
                    CLIENT_ID_KEY + "=",
                    "inboxpilot.oauth.client-secret=");
        }

        @Test
        @DisplayName("point at the environment variable that supplies them")
        void namesTheEnvironmentVariable() {
            assertStartupFailsNaming("INBOXPILOT_OAUTH_CLIENT_ID",
                    "inboxpilot.oauth.enabled=true",
                    CLIENT_ID_KEY + "=",
                    "inboxpilot.oauth.client-secret=");
        }

        @Test
        @DisplayName("are not demanded while OAuth is disabled")
        void allowsBlankCredentialsWhenDisabled() {
            try (ConfigurableApplicationContext context = startWith(
                    "inboxpilot.oauth.enabled=false",
                    CLIENT_ID_KEY + "=",
                    "inboxpilot.oauth.client-secret=")) {
                assertThat(context.isRunning()).isTrue();
            }
        }
    }

    /**
     * Asserts that startup fails and that the failure text identifies the
     * offending setting, so an operator can map the error back to the key they
     * need to fix.
     *
     * <p>Spring reports binding errors using the Java property path
     * ({@code batching.batchSize}) rather than the YAML key
     * ({@code inboxpilot.batching.batch-size}), so the expected text is
     * normalised to a comparable form before matching. Both spellings are
     * accepted; what matters is that the setting is named at all.
     */
    private static void assertStartupFailsNaming(String expectedInMessage, String... properties) {
        assertThatThrownBy(() -> startWith(properties).close())
                .as("startup must fail when %s is invalid", expectedInMessage)
                .isInstanceOf(Exception.class)
                .satisfies(thrown -> assertThat(normalise(rootMessages(thrown)))
                        .as("the failure must name the offending setting")
                        .contains(normalise(expectedInMessage)));
    }

    /**
     * Strips the {@code inboxpilot.} prefix and every hyphen and underscore, so
     * {@code inboxpilot.batching.batch-size} and {@code batching.batchSize}
     * compare equal.
     */
    private static String normalise(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace("inboxpilot.", "")
                .replace("-", "")
                .replace("_", "");
    }

    /**
     * Concatenates the messages of the whole cause chain. Spring wraps binding
     * failures several layers deep, and the useful detail can sit at any level.
     */
    private static String rootMessages(Throwable thrown) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(System.lineSeparator());
            if (current instanceof BindValidationException bindFailure) {
                messages.append(bindFailure.getValidationErrors()).append(System.lineSeparator());
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return messages.toString();
    }

    private static ConfigurableApplicationContext startWith(String... properties) {
        SpringApplication application = new SpringApplication(InboxPilotApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application.run(toArguments(properties));
    }

    private static String[] toArguments(String... properties) {
        String[] arguments = new String[properties.length];
        for (int index = 0; index < properties.length; index++) {
            arguments[index] = "--" + properties[index];
        }
        return arguments;
    }
}
