package dev.inboxpilot.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Which messages a scan covers.
 *
 * <p>Bounds matter here: an unbounded scan of a large mailbox burns API quota
 * and takes hours, so {@code maxMessages} is capped and validated rather than
 * left to a typo in YAML.
 *
 * @param lookback     how far back to scan; must be positive
 * @param maxMessages  hard ceiling on messages examined in one run
 * @param includeSpam  whether to include the spam folder
 * @param includeTrash whether to include the trash folder
 */
public record ScanningProperties(
        @NotNull(message = "must be set, e.g. 365d or 30d")
        Duration lookback,
        @Min(value = MIN_MESSAGES, message = "must scan at least {value} message")
        @Max(value = MAX_MESSAGES, message = "must not exceed {value} messages in one run")
        int maxMessages,
        boolean includeSpam,
        boolean includeTrash) {

    /** A scan that examines no messages is always a misconfiguration. */
    static final int MIN_MESSAGES = 1;

    /** Guards against a runaway scan exhausting the daily API quota. */
    static final int MAX_MESSAGES = 1_000_000;
}
