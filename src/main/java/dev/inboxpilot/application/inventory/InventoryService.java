package dev.inboxpilot.application.inventory;

import dev.inboxpilot.application.port.InventoryReportStore;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryAggregator;
import dev.inboxpilot.domain.message.MailMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Orchestrates the read-only mailbox inventory workflow. */
public final class InventoryService {

    private final MessageSource messageSource;
    private final InventoryReportStore reportStore;
    private final Duration lookback;
    private final int maximumMessages;
    private final Clock clock;
    private final InventoryAggregator aggregator = new InventoryAggregator();

    public InventoryService(
            MessageSource messageSource,
            InventoryReportStore reportStore,
            Duration lookback,
            int maximumMessages,
            Clock clock) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
        this.reportStore = Objects.requireNonNull(reportStore, "reportStore");
        this.lookback = Objects.requireNonNull(lookback, "lookback");
        this.maximumMessages = maximumMessages;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public InventoryRunResult run() {
        Instant since = clock.instant().minus(lookback);
        List<MailMessage> messages = messageSource.fetchSince(since, maximumMessages);
        Inventory inventory = aggregator.aggregate(messages);
        return new InventoryRunResult(messages.size(), reportStore.write(inventory));
    }
}
