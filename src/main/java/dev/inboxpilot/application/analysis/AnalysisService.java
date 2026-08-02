package dev.inboxpilot.application.analysis;

import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.domain.analysis.UnclassifiedInventory;
import dev.inboxpilot.domain.analysis.UnclassifiedInventoryAnalyzer;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Orchestrates deterministic, read-only mailbox analysis. */
public final class AnalysisService {

    private static final String USER_LABEL_PREFIX = "Label_";

    private final InventorySnapshotStore inventoryStore;
    private final UnclassifiedInventoryAnalyzer unclassifiedAnalyzer =
            new UnclassifiedInventoryAnalyzer();

    public AnalysisService(InventorySnapshotStore inventoryStore) {
        this.inventoryStore = Objects.requireNonNull(inventoryStore, "inventoryStore");
    }

    public AnalysisRunResult run() {
        Inventory inventory = inventoryStore.load();
        Set<String> userLabelIds = inventory.senders().stream()
                .map(sender -> sender.statistics().currentLabels())
                .flatMap(Collection::stream)
                .filter(AnalysisService::isUserLabel)
                .collect(Collectors.toUnmodifiableSet());
        UnclassifiedInventory unclassified = unclassifiedAnalyzer.analyze(
                inventory, userLabelIds);
        int processedMessages = inventory.senders().stream()
                .map(sender -> sender.statistics())
                .mapToInt(InventoryStatistics::messageCount)
                .sum();
        return new AnalysisRunResult(
                processedMessages,
                unclassified.senders().size(),
                unclassified.domains().size());
    }

    private static boolean isUserLabel(String labelId) {
        return labelId.startsWith(USER_LABEL_PREFIX);
    }
}
