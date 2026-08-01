package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GmailRetryPolicy")
class GmailRetryPolicyTest {

    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(5);
    private static final double NO_JITTER = 1.0;

    @Test
    @DisplayName("uses capped exponential backoff")
    void usesCappedExponentialBackoff() {
        GmailRetryPolicy policy = new GmailRetryPolicy(
                MAX_RETRIES, INITIAL_BACKOFF, MAX_BACKOFF, () -> NO_JITTER);

        assertThat(policy.delayBeforeRetry(1)).isEqualTo(INITIAL_BACKOFF);
        assertThat(policy.delayBeforeRetry(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(policy.delayBeforeRetry(3)).isEqualTo(MAX_BACKOFF);
    }
}
