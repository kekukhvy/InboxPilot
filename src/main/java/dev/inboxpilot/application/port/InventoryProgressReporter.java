package dev.inboxpilot.application.port;

import dev.inboxpilot.application.model.InventoryProgress;

/** Receives progress snapshots without coupling the scan to a presentation technology. */
@FunctionalInterface
public interface InventoryProgressReporter {

    void report(InventoryProgress progress);
}
