package dev.inboxpilot.application.inventory;

import dev.inboxpilot.application.port.InventoryReportStore;
import dev.inboxpilot.application.port.MessageSnapshotStore;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.model.ScanCheckpoint;
import dev.inboxpilot.application.model.ScanFingerprint;
import dev.inboxpilot.application.port.CheckpointStore;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryAggregator;
import dev.inboxpilot.domain.message.MailMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

/** Orchestrates the read-only mailbox inventory workflow. */
public final class InventoryService {

    private static final String FINGERPRINT_SEPARATOR = "|";

    private final MessageSource messageSource;
    private final InventoryReportStore reportStore;
    private final MessageSnapshotStore snapshotStore;
    private final CheckpointStore checkpointStore;
    private final boolean checkpointEnabled;
    private final Duration lookback;
    private final int maximumMessages;
    private final Clock clock;
    private final InventoryAggregator aggregator = new InventoryAggregator();

    public InventoryService(
            MessageSource messageSource,
            InventoryReportStore reportStore,
            MessageSnapshotStore snapshotStore,
            CheckpointStore checkpointStore,
            boolean checkpointEnabled,
            Duration lookback,
            int maximumMessages,
            Clock clock) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
        this.reportStore = Objects.requireNonNull(reportStore, "reportStore");
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore");
        this.checkpointStore = Objects.requireNonNull(checkpointStore, "checkpointStore");
        this.checkpointEnabled = checkpointEnabled;
        this.lookback = Objects.requireNonNull(lookback, "lookback");
        this.maximumMessages = maximumMessages;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public InventoryRunResult run() {
        String fingerprint = fingerprint();
        ScanCheckpoint restored = restore(fingerprint);
        Instant since = restored.since();
        List<MailMessage> messages = new ArrayList<>(restored.messages());
        Set<dev.inboxpilot.domain.message.MessageId> completedIds = messages.stream()
                .map(MailMessage::id)
                .collect(Collectors.toUnmodifiableSet());
        messageSource.fetchSince(since, maximumMessages, completedIds, batch -> {
            messages.addAll(batch);
            save(fingerprint, since, messages);
            snapshotStore.write(messages);
        });
        // A resumed run that fetches nothing new never reaches the batch
        // callback, so the snapshot is written once more here: finishing a scan
        // must always leave a snapshot matching the aggregates just reported.
        snapshotStore.write(messages);
        Inventory inventory = aggregator.aggregate(messages);
        InventoryRunResult result = new InventoryRunResult(
                messages.size(), reportStore.write(inventory));
        resetCheckpoint();
        return result;
    }

    private ScanCheckpoint restore(String fingerprint) {
        if (checkpointEnabled) {
            return checkpointStore.load(fingerprint)
                    .orElseGet(() -> emptyCheckpoint(fingerprint));
        }
        return emptyCheckpoint(fingerprint);
    }

    private ScanCheckpoint emptyCheckpoint(String fingerprint) {
        return new ScanCheckpoint(fingerprint, clock.instant().minus(lookback), List.of());
    }

    private void save(String fingerprint, Instant since, List<MailMessage> messages) {
        if (checkpointEnabled) {
            checkpointStore.save(new ScanCheckpoint(fingerprint, since, messages));
        }
    }

    private void resetCheckpoint() {
        if (checkpointEnabled) {
            checkpointStore.reset();
        }
    }

    private String fingerprint() {
        String canonical = lookback + FINGERPRINT_SEPARATOR + maximumMessages;
        return ScanFingerprint.fromCanonical(canonical);
    }
}
