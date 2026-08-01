package dev.inboxpilot.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.inventory.InventoryService;
import dev.inboxpilot.application.analysis.AnalysisService;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.application.model.ScanCheckpoint;
import dev.inboxpilot.application.port.CheckpointStore;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain JUnit with a hand-written stub rather than a Spring context: that the
 * CLI can be exercised without one is the property ADR 0001 protects, and a
 * test that needed the real adapter would prove the boundary had leaked.
 */
@DisplayName("InboxPilotCommand")
class InboxPilotCommandTest {

    private static final String LABEL_ID = "Label_42";
    private static final String LABEL_NAME = "Receipts";
    private static final String LABEL_LINE = LABEL_ID + "\t" + LABEL_NAME;
    private static final String MESSAGE_ID_VALUE = "message-42";
    private static final String QUERY = "after:2026/07/01 -in:trash";
    private static final String LABELS_ARGUMENT = "labels";
    private static final String MESSAGES_ARGUMENT = "messages";
    private static final String LIST_ARGUMENT = "list";
    private static final String QUERY_ARGUMENT = "--query=" + QUERY;
    private static final String UNKNOWN_ARGUMENT = "unknown";
    private static final String CHECKPOINT_ARGUMENT = "checkpoint";
    private static final String INVENTORY_ARGUMENT = "inventory";
    private static final String ANALYZE_ARGUMENT = "analyze";
    private static final String RESET_ARGUMENT = "reset";
    private static final String RESET_CONFIRMATION = "Checkpoint reset";
    private static final String USAGE_FRAGMENT = "labels list";

    private final MessageSource stubSource = since -> List.of();
    private final StubMailboxGateway stubGateway = new StubMailboxGateway();
    private final StubCheckpointStore checkpointStore = new StubCheckpointStore();

    @Test
    @DisplayName("reads through whichever port implementation it was given")
    void readsThroughTheInjectedPort() {
        InboxPilotCommand command = command();

        assertThat(command.messageSource()).isSameAs(stubSource);
    }

    @Test
    @DisplayName("depends on the port, not on any concrete adapter")
    void dependsOnThePortNotAnAdapter() {
        // Asserted against the constructor signature, because this is the one
        // property that keeps the CLI unaware of which provider is configured.
        Constructor<?>[] constructors = InboxPilotCommand.class.getDeclaredConstructors();

        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterTypes())
                .containsExactly(
                        MessageSource.class,
                        MailboxGateway.class,
                        CheckpointStore.class,
                        InventoryService.class,
                        AnalysisService.class);
    }

    @Test
    @DisplayName("works with any port implementation, not just the Gmail one")
    void worksWithAnyImplementation() {
        MailMessage message = new MailMessage(
                new MessageId("msg-1"),
                new EmailAddress("news@example.com"),
                "Weekly digest",
                Instant.parse("2026-08-01T10:15:30Z"),
                List.of());
        MessageSource inMemorySource = since -> List.of(message);

        InboxPilotCommand command = new InboxPilotCommand(
                inMemorySource, stubGateway, checkpointStore,
                inventoryService(inMemorySource), analysisService(inMemorySource));
        command.reportConfiguredSource();

        assertThat(command.messageSource().fetchSince(Instant.EPOCH))
                .containsExactly(message);
    }

    @Test
    @DisplayName("lists real mailbox labels through the gateway")
    void listsLabels() {
        stubGateway.labels = List.of(new MailboxLabel(LABEL_ID, LABEL_NAME));
        InboxPilotCommand command = command();

        assertThat(command.execute(LABELS_ARGUMENT, LIST_ARGUMENT)).containsExactly(LABEL_LINE);
    }

    @Test
    @DisplayName("lists message ids using the requested Gmail query")
    void listsMessageIds() {
        stubGateway.messageIds = List.of(new MessageId(MESSAGE_ID_VALUE));
        InboxPilotCommand command = command();

        assertThat(command.execute(MESSAGES_ARGUMENT, LIST_ARGUMENT, QUERY_ARGUMENT))
                .containsExactly(MESSAGE_ID_VALUE);
        assertThat(stubGateway.query).isEqualTo(QUERY);
    }

    @Test
    @DisplayName("rejects an unknown command with actionable usage")
    void rejectsUnknownCommand() {
        InboxPilotCommand command = command();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> command.execute(UNKNOWN_ARGUMENT))
                .withMessageContaining(USAGE_FRAGMENT);
    }

    @Test
    @DisplayName("resets checkpoint only through the explicit command")
    void resetsCheckpointExplicitly() {
        assertThat(command().execute(CHECKPOINT_ARGUMENT, RESET_ARGUMENT))
                .containsExactly(RESET_CONFIRMATION);
        assertThat(checkpointStore.wasReset).isTrue();
    }

    @Test
    @DisplayName("runs inventory and renders generated report paths")
    void runsInventory() {
        assertThat(command().execute(INVENTORY_ARGUMENT))
                .containsExactly("Inventory complete: 0 messages", "Report: /tmp/inventory.json");
    }

    @Test
    @DisplayName("runs read-only mailbox analysis")
    void runsAnalysis() {
        assertThat(command().execute(ANALYZE_ARGUMENT))
                .containsExactly("Analysis complete: 0 messages, 0 unclassified senders, "
                        + "0 unclassified domains, 0 unused labels, 0 duplicate label groups");
    }

    private InboxPilotCommand command() {
        return new InboxPilotCommand(
                stubSource, stubGateway, checkpointStore,
                inventoryService(stubSource), analysisService(stubSource));
    }

    private AnalysisService analysisService(MessageSource source) {
        return new AnalysisService(
                source, stubGateway, Duration.ofDays(1), 10,
                Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC));
    }

    private static InventoryService inventoryService(MessageSource source) {
        return new InventoryService(
                source,
                inventory -> List.of(java.nio.file.Path.of("/tmp/inventory.json")),
                new StubCheckpointStore(),
                false,
                Duration.ofDays(1),
                10,
                Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC));
    }

    private static final class StubCheckpointStore implements CheckpointStore {
        private boolean wasReset;

        @Override
        public Optional<ScanCheckpoint> load(String expectedFingerprint) {
            return Optional.empty();
        }

        @Override
        public void save(ScanCheckpoint checkpoint) {
        }

        @Override
        public void reset() {
            wasReset = true;
        }
    }

    private static final class StubMailboxGateway implements MailboxGateway {
        private List<MailboxLabel> labels = List.of();
        private List<MessageId> messageIds = List.of();
        private String query;

        @Override
        public List<MailboxLabel> listLabels() {
            return labels;
        }

        @Override
        public List<MessageId> listMessageIds(String requestedQuery) {
            query = requestedQuery;
            return messageIds;
        }
    }
}
