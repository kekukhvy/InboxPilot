package dev.inboxpilot.application.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.InventoryProgress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InventoryProgressTracker")
class InventoryProgressTrackerTest {

    private static final int TOTAL_MESSAGES = 10;
    private static final int PROCESSED_MESSAGES = 4;
    private static final int REMAINING_MESSAGES = 6;
    private static final int RETRY_COUNT = 1;
    private static final Duration ELAPSED = Duration.ofSeconds(2);
    private static final Duration EXPECTED_ETA = Duration.ofSeconds(3);
    private static final double EXPECTED_THROUGHPUT = 2.0;
    private static final Instant START = Instant.parse("2026-08-01T10:00:00Z");

    @Test
    @DisplayName("reports counts, throughput, elapsed time, and ETA")
    void reportsCompleteProgress() {
        MutableClock clock = new MutableClock(START);
        List<InventoryProgress> reports = new ArrayList<>();
        InventoryProgressTracker tracker = new InventoryProgressTracker(
                TOTAL_MESSAGES, reports::add, clock);

        tracker.recordRetry();
        clock.advance(ELAPSED);
        tracker.recordProcessed(PROCESSED_MESSAGES);

        InventoryProgress progress = reports.getLast();
        assertThat(progress.processedMessages()).isEqualTo(PROCESSED_MESSAGES);
        assertThat(progress.remainingMessages()).isEqualTo(REMAINING_MESSAGES);
        assertThat(progress.retryCount()).isEqualTo(RETRY_COUNT);
        assertThat(progress.messagesPerSecond()).isEqualTo(EXPECTED_THROUGHPUT);
        assertThat(progress.elapsed()).isEqualTo(ELAPSED);
        assertThat(progress.eta()).contains(EXPECTED_ETA);
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
