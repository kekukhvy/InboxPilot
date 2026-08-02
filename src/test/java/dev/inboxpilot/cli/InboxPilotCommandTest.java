package dev.inboxpilot.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.model.RuleFileSettings;
import dev.inboxpilot.application.classification.ClassificationDryRunService;
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
import java.nio.file.Path;
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
    private static final String CLASSIFY_ARGUMENT = "classify";
    private static final String DRY_RUN_ARGUMENT = "--dry-run";
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
                        AnalysisService.class,
                        ClassificationDryRunService.class,
                        RuleFileSettings.class);
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
                inventoryService(inMemorySource), analysisService(inMemorySource),
                classificationService(inMemorySource), ruleFileSettings());
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
                .containsExactly(
                        "Analysis complete: 0 messages, 0 unclassified senders, "
                                + "0 unclassified domains",
                        "Report: /tmp/summary.json");
    }

    @Test
    void runsClassificationOnlyAsAnExplicitDryRun() {
        assertThat(command().execute(CLASSIFY_ARGUMENT, DRY_RUN_ARGUMENT))
                .containsExactly(
                        "Classification dry-run complete: 0 processed, 0 matched, 0 changes, 0 conflicts",
                        "Report: /tmp/classification-dry-run-summary.json");
    }

    @Test
    void rejectsClassificationWithoutDryRun() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> command().execute(CLASSIFY_ARGUMENT))
                .withMessageContaining("--dry-run");
    }

    private InboxPilotCommand command() {
        return new InboxPilotCommand(
                stubSource, stubGateway, checkpointStore,
                inventoryService(stubSource), analysisService(stubSource),
                classificationService(stubSource), ruleFileSettings());
    }

    private AnalysisService analysisService(MessageSource source) {
        return new AnalysisService(() -> new dev.inboxpilot.domain.inventory.Inventory(
                List.of(), List.of()), report -> List.of(
                        java.nio.file.Path.of("/tmp/summary.json")), stubGateway,
                new dev.inboxpilot.domain.rules.RuleGenerationPolicy(
                        java.util.Set.of(), java.util.Map.of(), java.util.Set.of(), 20));
    }

    private ClassificationDryRunService classificationService(MessageSource source) {
        return new ClassificationDryRunService(
                () -> new dev.inboxpilot.domain.inventory.Inventory(List.of(), List.of()),
                ignored -> new dev.inboxpilot.domain.rules.RuleSet(1, List.of()),
                source,
                stubGateway,
                report -> List.of(Path.of("/tmp/classification-dry-run-summary.json")),
                new dev.inboxpilot.domain.rules.RuleGenerationPolicy(
                        java.util.Set.of(), java.util.Map.of(), java.util.Set.of(), 20));
    }

    private static RuleFileSettings ruleFileSettings() {
        return new RuleFileSettings(Path.of("/tmp/rules/approved-rules.yaml"));
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
