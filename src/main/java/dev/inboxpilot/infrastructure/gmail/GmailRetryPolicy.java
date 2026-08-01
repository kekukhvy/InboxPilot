package dev.inboxpilot.infrastructure.gmail;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

final class GmailRetryPolicy {

    private static final long EXPONENTIAL_BASE = 2L;
    private static final double MIN_JITTER = 0.5;
    private static final double JITTER_RANGE = 1.0;

    private final int maxRetries;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final DoubleSupplier jitter;

    GmailRetryPolicy(int maxRetries, Duration initialBackoff, Duration maxBackoff) {
        this(maxRetries, initialBackoff, maxBackoff,
                () -> MIN_JITTER + ThreadLocalRandom.current().nextDouble() * JITTER_RANGE);
    }

    GmailRetryPolicy(
            int maxRetries,
            Duration initialBackoff,
            Duration maxBackoff,
            DoubleSupplier jitter) {
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.jitter = jitter;
    }

    boolean canRetry(int retriesCompleted) {
        return retriesCompleted < maxRetries;
    }

    Duration delayBeforeRetry(int retryNumber) {
        long multiplier = powerOfTwo(retryNumber - 1);
        Duration exponential = multiplySafely(initialBackoff, multiplier);
        Duration capped = exponential.compareTo(maxBackoff) > 0 ? maxBackoff : exponential;
        return Duration.ofMillis(Math.round(capped.toMillis() * jitter.getAsDouble()));
    }

    private static long powerOfTwo(int exponent) {
        int safeExponent = Math.min(exponent, Long.SIZE - 2);
        return 1L << safeExponent;
    }

    private static Duration multiplySafely(Duration duration, long multiplier) {
        try {
            return duration.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            return Duration.ofMillis(Long.MAX_VALUE);
        }
    }
}
