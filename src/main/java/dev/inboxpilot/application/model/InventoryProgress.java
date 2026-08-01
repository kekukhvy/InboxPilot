package dev.inboxpilot.application.model;

import java.time.Duration;
import java.util.Optional;

/** Immutable progress snapshot for a mailbox inventory scan. */
public record InventoryProgress(
        int processedMessages,
        int remainingMessages,
        int retryCount,
        double messagesPerSecond,
        Duration elapsed,
        Optional<Duration> eta) {
}
