package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import java.util.Comparator;
import java.util.Set;

/** Finds and ranks inventory entries that have no user-created label. */
public final class UnclassifiedInventoryAnalyzer {
    private static final Comparator<InventoryStatistics> STATISTICS_RANKING =
            Comparator.comparingInt(InventoryStatistics::messageCount).reversed()
                    .thenComparing(Comparator.comparingInt(
                            InventoryStatistics::unreadCount).reversed());
    private static final Comparator<SenderInventory> SENDER_RANKING =
            Comparator.comparing(SenderInventory::statistics, STATISTICS_RANKING)
                    .thenComparing(entry -> entry.sender().value());
    private static final Comparator<DomainInventory> DOMAIN_RANKING =
            Comparator.comparing(DomainInventory::statistics, STATISTICS_RANKING)
                    .thenComparing(DomainInventory::domain);

    public UnclassifiedInventory analyze(Inventory inventory, Set<String> userLabelIds) {
        return new UnclassifiedInventory(
                inventory.senders().stream()
                        .filter(entry -> hasNoUserLabel(entry.statistics(), userLabelIds))
                        .sorted(SENDER_RANKING).toList(),
                inventory.domains().stream()
                        .filter(entry -> hasNoUserLabel(entry.statistics(), userLabelIds))
                        .sorted(DOMAIN_RANKING).toList());
    }

    private static boolean hasNoUserLabel(
            InventoryStatistics statistics, Set<String> userLabelIds) {
        return statistics.currentLabels().stream().noneMatch(userLabelIds::contains);
    }
}
