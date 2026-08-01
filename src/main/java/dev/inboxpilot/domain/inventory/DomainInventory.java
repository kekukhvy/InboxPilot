package dev.inboxpilot.domain.inventory;

/** Inventory statistics for all senders belonging to a domain. */
public record DomainInventory(String domain, InventoryStatistics statistics) {
}
