package dev.inboxpilot.application.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.ScanCheckpoint;
import dev.inboxpilot.application.port.CheckpointStore;
import dev.inboxpilot.application.port.InventoryReportStore;
import dev.inboxpilot.application.port.MessageSnapshotStore;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");
    private static final Duration LOOKBACK = Duration.ofDays(30);
    private static final int MAXIMUM_MESSAGES = 2;
    private static final String FIRST_ID = "one";
    private static final String SECOND_ID = "two";
    private static final String RESTORED_FINGERPRINT = "ignored";
    private static final String REPORT_FAILURE = "report failure";

    @Test
    void scansConfiguredLookbackCapsMessagesAndWritesReports() {
        CapturingSource source = new CapturingSource(List.of(
                message(FIRST_ID), message(SECOND_ID), message("three")));
        CapturingReports reports = new CapturingReports();
        InventoryService service = new InventoryService(
                source, reports, new CapturingSnapshotStore(),
                new CapturingCheckpointStore(), true,
                LOOKBACK, MAXIMUM_MESSAGES, Clock.fixed(NOW, ZoneOffset.UTC));

        InventoryRunResult result = service.run();

        assertThat(source.since).isEqualTo(NOW.minus(LOOKBACK));
        assertThat(source.maximumMessages).isEqualTo(MAXIMUM_MESSAGES);
        assertThat(result.processedMessages()).isEqualTo(MAXIMUM_MESSAGES);
        assertThat(result.reports()).containsExactly(Path.of("reports/inventory.json"));
        assertThat(reports.inventory.senders()).hasSize(2);
    }

    @Test
    void savesEveryBatchAndResumesWithoutFetchingCompletedMessages() {
        MailMessage restored = message(FIRST_ID);
        MailMessage newlyFetched = message(SECOND_ID);
        CapturingCheckpointStore checkpoints = new CapturingCheckpointStore();
        checkpoints.saved = new ScanCheckpoint(
                RESTORED_FINGERPRINT, NOW.minus(LOOKBACK), List.of(restored));
        CapturingSource source = new CapturingSource(List.of(newlyFetched));
        InventoryService service = new InventoryService(
                source, new CapturingReports(), new CapturingSnapshotStore(), checkpoints, true,
                LOOKBACK, MAXIMUM_MESSAGES, Clock.fixed(NOW, ZoneOffset.UTC));

        InventoryRunResult result = service.run();

        assertThat(source.completedIds).containsExactly(restored.id());
        assertThat(checkpoints.saved.messages()).containsExactly(restored, newlyFetched);
        assertThat(checkpoints.saveCount).isEqualTo(1);
        assertThat(checkpoints.wasReset).isTrue();
        assertThat(result.processedMessages()).isEqualTo(MAXIMUM_MESSAGES);
    }

    @Test
    void preservesCheckpointWhenReportWritingFails() {
        CapturingCheckpointStore checkpoints = new CapturingCheckpointStore();
        CapturingSource source = new CapturingSource(List.of(message(FIRST_ID)));
        InventoryReportStore failingReports = inventory -> {
            throw new IllegalStateException(REPORT_FAILURE);
        };
        InventoryService service = new InventoryService(
                source, failingReports, new CapturingSnapshotStore(), checkpoints, true,
                LOOKBACK, MAXIMUM_MESSAGES, Clock.fixed(NOW, ZoneOffset.UTC));

        org.assertj.core.api.Assertions.assertThatThrownBy(service::run)
                .isInstanceOf(IllegalStateException.class);

        assertThat(checkpoints.saved.messages()).hasSize(1);
        assertThat(checkpoints.wasReset).isFalse();
    }

    @Test
    void preservesCompletedBatchWhenLaterFetchingFails() {
        CapturingCheckpointStore checkpoints = new CapturingCheckpointStore();
        MailMessage completed = message(FIRST_ID);
        MessageSource interruptedSource = new MessageSource() {
            @Override
            public List<MailMessage> fetchSince(Instant since) {
                return List.of();
            }

            @Override
            public List<MailMessage> fetchSince(
                    Instant since,
                    int maximumMessages,
                    java.util.Set<MessageId> completedIds,
                    java.util.function.Consumer<List<MailMessage>> completedBatch) {
                completedBatch.accept(List.of(completed));
                throw new IllegalStateException(REPORT_FAILURE);
            }
        };
        InventoryService service = new InventoryService(
                interruptedSource, new CapturingReports(), new CapturingSnapshotStore(),
                checkpoints, true,
                LOOKBACK, MAXIMUM_MESSAGES, Clock.fixed(NOW, ZoneOffset.UTC));

        org.assertj.core.api.Assertions.assertThatThrownBy(service::run)
                .isInstanceOf(IllegalStateException.class);

        assertThat(checkpoints.saved.messages()).containsExactly(completed);
        assertThat(checkpoints.wasReset).isFalse();
    }

    @Test
    void persistsTheMessageSnapshotClassificationLaterReadsWithoutRescanning() {
        CapturingSnapshotStore snapshots = new CapturingSnapshotStore();
        CapturingSource source = new CapturingSource(
                List.of(message(FIRST_ID), message(SECOND_ID)));
        InventoryService service = new InventoryService(
                source, new CapturingReports(), snapshots, new CapturingCheckpointStore(), true,
                LOOKBACK, MAXIMUM_MESSAGES, Clock.fixed(NOW, ZoneOffset.UTC));

        service.run();

        assertThat(snapshots.load())
                .extracting(message -> message.id().value())
                .containsExactly(FIRST_ID, SECOND_ID);
    }

    @Test
    void persistsTheSnapshotAtCompletedBatchBoundariesSoInterruptionKeepsFinishedWork() {
        CapturingSnapshotStore snapshots = new CapturingSnapshotStore();
        MailMessage completed = message(FIRST_ID);
        MessageSource interruptedSource = new MessageSource() {
            @Override
            public List<MailMessage> fetchSince(Instant since) {
                return List.of();
            }

            @Override
            public List<MailMessage> fetchSince(
                    Instant since,
                    int maximumMessages,
                    java.util.Set<MessageId> completedIds,
                    java.util.function.Consumer<List<MailMessage>> completedBatch) {
                completedBatch.accept(List.of(completed));
                throw new IllegalStateException(REPORT_FAILURE);
            }
        };
        InventoryService service = new InventoryService(
                interruptedSource, new CapturingReports(), snapshots,
                new CapturingCheckpointStore(), true,
                LOOKBACK, MAXIMUM_MESSAGES, Clock.fixed(NOW, ZoneOffset.UTC));

        org.assertj.core.api.Assertions.assertThatThrownBy(service::run)
                .isInstanceOf(IllegalStateException.class);

        assertThat(snapshots.writes).containsExactly(List.of(completed));
    }

    @Test
    void persistsARestoredSnapshotEvenWhenNoNewMessagesAreFetched() {
        CapturingSnapshotStore snapshots = new CapturingSnapshotStore();
        MailMessage restored = message(FIRST_ID);
        CapturingCheckpointStore checkpoints = new CapturingCheckpointStore();
        checkpoints.saved = new ScanCheckpoint(
                RESTORED_FINGERPRINT, NOW.minus(LOOKBACK), List.of(restored));
        InventoryService service = new InventoryService(
                new CapturingSource(List.of(restored)), new CapturingReports(), snapshots,
                checkpoints, true,
                LOOKBACK, MAXIMUM_MESSAGES, Clock.fixed(NOW, ZoneOffset.UTC));

        service.run();

        assertThat(snapshots.load()).containsExactly(restored);
    }

    private static MailMessage message(String id) {
        return new MailMessage(
                new MessageId(id), new EmailAddress(id + "@example.com"), id,
                NOW.minusSeconds(60), List.of());
    }

    private static final class CapturingSource implements MessageSource {
        private final List<MailMessage> messages;
        private Instant since;
        private int maximumMessages;
        private java.util.Set<MessageId> completedIds = java.util.Set.of();

        private CapturingSource(List<MailMessage> messages) {
            this.messages = new ArrayList<>(messages);
        }

        @Override
        public List<MailMessage> fetchSince(Instant requestedSince) {
            since = requestedSince;
            return List.copyOf(messages);
        }

        @Override
        public List<MailMessage> fetchSince(Instant requestedSince, int requestedMaximum) {
            since = requestedSince;
            maximumMessages = requestedMaximum;
            return messages.stream().limit(requestedMaximum).toList();
        }


        @Override
        public List<MailMessage> fetchSince(
                Instant requestedSince,
                int requestedMaximum,
                java.util.Set<MessageId> requestedCompletedIds,
                java.util.function.Consumer<List<MailMessage>> completedBatch) {
            since = requestedSince;
            maximumMessages = requestedMaximum;
            completedIds = java.util.Set.copyOf(requestedCompletedIds);
            List<MailMessage> fetched = messages.stream()
                    .filter(message -> !completedIds.contains(message.id()))
                    .limit(requestedMaximum - completedIds.size())
                    .toList();
            if (!fetched.isEmpty()) {
                completedBatch.accept(fetched);
            }
            return fetched;
        }
    }

    /** Snapshot fixture recording every batch-boundary write in order. */
    private static final class CapturingSnapshotStore implements MessageSnapshotStore {
        private final List<List<MailMessage>> writes = new ArrayList<>();

        @Override
        public void write(List<MailMessage> messages) {
            writes.add(List.copyOf(messages));
        }

        @Override
        public List<MailMessage> load() {
            return writes.isEmpty() ? List.of() : writes.get(writes.size() - 1);
        }
    }

    private static final class CapturingReports implements InventoryReportStore {
        private Inventory inventory;

        @Override
        public List<Path> write(Inventory requestedInventory) {
            inventory = requestedInventory;
            return List.of(Path.of("reports/inventory.json"));
        }
    }


    private static final class CapturingCheckpointStore implements CheckpointStore {
        private ScanCheckpoint saved;
        private int saveCount;
        private boolean wasReset;

        @Override
        public Optional<ScanCheckpoint> load(String expectedFingerprint) {
            return Optional.ofNullable(saved).map(checkpoint ->
                    new ScanCheckpoint(expectedFingerprint, checkpoint.since(), checkpoint.messages()));
        }

        @Override
        public void save(ScanCheckpoint checkpoint) {
            saved = checkpoint;
            saveCount++;
        }

        @Override
        public void reset() {
            wasReset = true;
        }
    }
}
