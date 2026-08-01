package dev.inboxpilot.infrastructure.gmail;

import java.io.IOException;
import java.time.Duration;

/** Paces sequential Gmail calls against the per-user quota-unit budget. */
final class GmailQuotaRateLimiter {

    static final int PER_USER_UNITS_PER_MINUTE = 6_000;
    static final int UTILIZATION_PERCENT = 80;
    static final int MESSAGE_GET_UNITS = 5;
    static final int MESSAGE_LIST_UNITS = 5;
    private static final long NANOS_PER_MINUTE = Duration.ofMinutes(1).toNanos();
    private static final int PERCENT = 100;

    private final RetrySleeper sleeper;

    GmailQuotaRateLimiter(RetrySleeper sleeper) {
        this.sleeper = sleeper;
    }

    void beforeMessageList() throws IOException {
        pauseForUnits(MESSAGE_LIST_UNITS);
    }

    void beforeMessageGetBatch(int messageCount) throws IOException {
        pauseForUnits(Math.multiplyExact(messageCount, MESSAGE_GET_UNITS));
    }

    Duration delayForUnits(int units) {
        long usableUnits = (long) PER_USER_UNITS_PER_MINUTE * UTILIZATION_PERCENT / PERCENT;
        return Duration.ofNanos(Math.multiplyExact(NANOS_PER_MINUTE, units) / usableUnits);
    }

    private void pauseForUnits(int units) throws IOException {
        try {
            sleeper.sleep(delayForUnits(units));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while pacing Gmail quota usage", exception);
        }
    }
}
