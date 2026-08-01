package dev.inboxpilot.domain.inventory;

import java.time.Instant;
import java.util.List;

/** Statistics shared by exact-sender and domain inventory entries. */
public record InventoryStatistics(
        int messageCount,
        int unreadCount,
        Instant firstReceivedAt,
        Instant lastReceivedAt,
        List<String> currentLabels,
        List<String> sampleSubjects) {

    public InventoryStatistics {
        currentLabels = List.copyOf(currentLabels);
        sampleSubjects = List.copyOf(sampleSubjects);
    }
}
