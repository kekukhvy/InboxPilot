package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.inventory.Inventory;
import java.nio.file.Path;
import java.util.List;

/** Persists a completed mailbox inventory in configured report formats. */
public interface InventoryReportStore {

    List<Path> write(Inventory inventory);
}
