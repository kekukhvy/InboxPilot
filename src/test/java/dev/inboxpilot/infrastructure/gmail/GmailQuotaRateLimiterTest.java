package dev.inboxpilot.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GmailQuotaRateLimiterTest {

    @Test
    void pacesEveryInnerMessageGetByQuotaUnits() throws Exception {
        List<Duration> delays = new ArrayList<>();
        GmailQuotaRateLimiter limiter = new GmailQuotaRateLimiter(delays::add);

        limiter.beforeMessageGetBatch(50);

        assertThat(delays).containsExactly(Duration.ofMillis(3_125));
    }

    @Test
    void pacesListCallsUsingTheirOwnQuotaCost() throws Exception {
        List<Duration> delays = new ArrayList<>();
        GmailQuotaRateLimiter limiter = new GmailQuotaRateLimiter(delays::add);

        limiter.beforeMessageList();

        assertThat(delays).containsExactly(Duration.ofMillis(62).plusNanos(500_000));
    }
}
