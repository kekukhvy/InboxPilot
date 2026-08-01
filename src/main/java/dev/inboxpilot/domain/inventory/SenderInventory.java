package dev.inboxpilot.domain.inventory;

import dev.inboxpilot.domain.message.EmailAddress;

/** Inventory statistics for one exact sender address. */
public record SenderInventory(EmailAddress sender, InventoryStatistics statistics) {
}
