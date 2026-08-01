package dev.inboxpilot.application.inventory;

import dev.inboxpilot.application.model.InventoryProgress;
import dev.inboxpilot.application.port.InventoryProgressReporter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Calculates and publishes inventory progress from monotonic count updates. */
public final class InventoryProgressTracker {

    private static final long MILLIS_PER_SECOND = 1_000L;

    private final int totalMessages;
    private final InventoryProgressReporter reporter;
    private final Clock clock;
    private final Instant startedAt;
    private int processedMessages;
    private int retryCount;

    public InventoryProgressTracker(
            int totalMessages,
            InventoryProgressReporter reporter,
            Clock clock) {
        this.totalMessages = totalMessages;
        this.reporter = reporter;
        this.clock = clock;
        this.startedAt = clock.instant();
    }

    public void recordProcessed(int count) {
        processedMessages = Math.min(totalMessages, processedMessages + count);
        publish();
    }

    public void recordRetry() {
        retryCount++;
        publish();
    }

    private void publish() {
        Duration elapsed = Duration.between(startedAt, clock.instant());
        double throughput = throughput(elapsed);
        int remaining = totalMessages - processedMessages;
        reporter.report(new InventoryProgress(
                processedMessages, remaining, retryCount, throughput,
                elapsed, eta(remaining, throughput)));
    }

    private double throughput(Duration elapsed) {
        if (elapsed.isZero() || elapsed.isNegative()) {
            return 0;
        }
        double seconds = (double) elapsed.toMillis() / MILLIS_PER_SECOND;
        return processedMessages / seconds;
    }

    private static Optional<Duration> eta(int remaining, double throughput) {
        if (throughput <= 0) {
            return Optional.empty();
        }
        long etaMillis = Math.round(remaining / throughput * MILLIS_PER_SECOND);
        return Optional.of(Duration.ofMillis(etaMillis));
    }
}
