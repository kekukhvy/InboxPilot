package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.SenderInventory;
import java.util.List;

/** Ranked sender and domain entries that carry no user-created label. */
public record UnclassifiedInventory(List<SenderInventory> senders, List<DomainInventory> domains) {
    public UnclassifiedInventory {
        senders = List.copyOf(senders);
        domains = List.copyOf(domains);
    }
}
