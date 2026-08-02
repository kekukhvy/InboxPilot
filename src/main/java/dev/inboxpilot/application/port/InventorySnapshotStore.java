package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.inventory.Inventory;

/** Loads the persisted inventory that downstream workflows analyze. */
public interface InventorySnapshotStore {

    Inventory load();
}
