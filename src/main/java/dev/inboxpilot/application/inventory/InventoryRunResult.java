package dev.inboxpilot.application.inventory;

import java.nio.file.Path;
import java.util.List;

/** Outcome rendered by the inventory CLI command. */
public record InventoryRunResult(int processedMessages, List<Path> reports) {

    public InventoryRunResult {
        reports = List.copyOf(reports);
    }
}
