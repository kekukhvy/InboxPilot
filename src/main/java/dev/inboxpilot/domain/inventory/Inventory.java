package dev.inboxpilot.domain.inventory;

import java.util.List;

/** Deterministic sender and domain views of a mailbox scan. */
public record Inventory(List<SenderInventory> senders, List<DomainInventory> domains) {

    public Inventory {
        senders = List.copyOf(senders);
        domains = List.copyOf(domains);
    }
}
