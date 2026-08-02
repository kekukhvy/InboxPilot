package dev.inboxpilot.application.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.application.port.MessageSnapshotStore;
import dev.inboxpilot.application.port.MessageSnapshotStoreException;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleActionSpec;
import dev.inboxpilot.domain.rules.RuleConditionSpec;
import dev.inboxpilot.domain.rules.RuleConflictBehavior;
import dev.inboxpilot.domain.rules.RuleDefinition;
import dev.inboxpilot.domain.rules.RuleGenerationPolicy;
import dev.inboxpilot.domain.rules.RuleId;
import dev.inboxpilot.domain.rules.RuleMetadata;
import dev.inboxpilot.domain.rules.RuleSet;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClassificationDryRunServiceTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final Path RULES_PATH = Path.of("rules.yaml");
    private static final String LABEL_ID = "Label_1";
    private static final String LABEL_NAME = "Accounts/Google";

    @Test
    void alreadyLabeledMessageDoesNotProduceAChange() {
        ClassificationDryRunReport report = run(
                List.of(rule("google", "sender-domain", "google.com", LABEL_NAME)),
                message(List.of(LABEL_ID)));

        assertThat(report.changes()).isEmpty();
        assertThat(report.summary().alreadyCorrectlyLabeled()).isOne();
    }

    @Test
    void identicalActionsFromMultipleRulesAreNotAConflict() {
        ClassificationDryRunReport report = run(List.of(
                rule("subject", "subject-contains", "Security", LABEL_NAME),
                rule("sender", "sender-exact", "news@google.com", LABEL_NAME)), message(List.of()));

        assertThat(report.conflicts()).isEmpty();
        assertThat(report.summary().messagesMatchedByMultipleRules()).isOne();
        assertThat(report.changes()).hasSize(1);
    }

    @Test
    void incompatibleActionsFromMultipleRulesAreReportedAsAConflict() {
        ClassificationDryRunReport report = run(List.of(
                rule("domain", "sender-domain", "google.com", LABEL_NAME),
                rule("sender", "sender-exact", "news@google.com", "Newsletters/Google")),
                message(List.of()));

        assertThat(report.conflicts()).hasSize(1);
        assertThat(report.changes()).isEmpty();
        assertThat(report.summary().ruleConflicts()).isOne();
    }

    @Test
    void validatesRulesBeforeReadingMailboxMetadata() {
        CountingGateway gateway = new CountingGateway();
        ClassificationDryRunService service = service(
                List.of(rule("unsafe", "sender-domain", "blocked.example", LABEL_NAME)),
                message(List.of()), gateway, ignored -> { });

        assertThatThrownBy(() -> service.run(RULES_PATH))
                .isInstanceOf(ClassificationRuleValidationException.class);
        assertThat(gateway.readCount).isZero();
    }

    @Test
    void constructorHasNoMutationPort() {
        assertThat(ClassificationDryRunService.class.getDeclaredConstructors()[0]
                .getParameterTypes())
                .noneMatch(type -> type.getSimpleName().contains("Change")
                        || type.getSimpleName().contains("Archive"));
    }

    @Test
    void constructorCannotTriggerAMailboxRescan() {
        assertThat(ClassificationDryRunService.class.getDeclaredConstructors()[0]
                .getParameterTypes())
                .doesNotContain(MessageSource.class);
    }

    @Test
    void readsMessagesFromTheSnapshotWithoutFetchingThem() {
        CountingSnapshotStore snapshot = new CountingSnapshotStore(message(List.of()));

        ClassificationDryRunReport report = run(
                List.of(rule("google", "sender-domain", "google.com", LABEL_NAME)), snapshot);

        assertThat(report.summary().processedMessages()).isOne();
        assertThat(snapshot.loadCount).isOne();
    }

    @Test
    void repeatedRunsAgainstAnUnchangedSnapshotProduceIdenticalReports() {
        CountingSnapshotStore snapshot = new CountingSnapshotStore(message(List.of()));
        List<RuleDefinition> rules =
                List.of(rule("google", "sender-domain", "google.com", LABEL_NAME));

        ClassificationDryRunReport first = run(rules, snapshot);
        ClassificationDryRunReport second = run(rules, snapshot);

        assertThat(second.summary()).isEqualTo(first.summary());
        assertThat(second.changes()).isEqualTo(first.changes());
    }

    @Test
    void missingSnapshotFailsWithAnActionableMigrationInstruction() {
        MessageSnapshotStore missing = new MessageSnapshotStore() {
            @Override
            public void write(List<MailMessage> messages) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<MailMessage> load() {
                throw new MessageSnapshotStoreException(
                        "Message snapshot is missing; rerun: inventory");
            }
        };
        ClassificationDryRunService service = service(
                List.of(rule("google", "sender-domain", "google.com", LABEL_NAME)),
                missing, new CountingGateway(), ignored -> { });

        assertThatThrownBy(() -> service.run(RULES_PATH))
                .isInstanceOf(MessageSnapshotStoreException.class)
                .hasMessageContaining("inventory");
    }

    private ClassificationDryRunReport run(List<RuleDefinition> rules, MailMessage message) {
        return run(rules, new CountingSnapshotStore(message));
    }

    private ClassificationDryRunReport run(
            List<RuleDefinition> rules, MessageSnapshotStore snapshot) {
        ClassificationDryRunReport[] captured = new ClassificationDryRunReport[1];
        service(rules, snapshot, new CountingGateway(), report -> captured[0] = report)
                .run(RULES_PATH);
        return captured[0];
    }

    private ClassificationDryRunService service(
            List<RuleDefinition> rules,
            MailMessage message,
            CountingGateway gateway,
            java.util.function.Consumer<ClassificationDryRunReport> capture) {
        return service(rules, new CountingSnapshotStore(message), gateway, capture);
    }

    private ClassificationDryRunService service(
            List<RuleDefinition> rules,
            MessageSnapshotStore snapshot,
            CountingGateway gateway,
            java.util.function.Consumer<ClassificationDryRunReport> capture) {
        return new ClassificationDryRunService(
                snapshot,
                ignored -> new RuleSet(RuleSet.CURRENT_VERSION, rules),
                gateway,
                report -> {
                    capture.accept(report);
                    return List.of(Path.of("summary.json"));
                },
                new RuleGenerationPolicy(
                        Set.of("blocked.example"), Map.of(), Set.of(), 1));
    }

    private static MailMessage message(List<String> labels) {
        return new MailMessage(new MessageId("message-1"),
                new EmailAddress("news@google.com"), "Security alert", RECEIVED_AT, labels);
    }

    private static RuleDefinition rule(
            String id, String operator, String value, String label) {
        return new RuleDefinition(new RuleId(id), 100,
                new RuleConditionSpec(operator, Map.of("value", value), List.of()),
                List.of(new RuleActionSpec("add-label", Map.of("label", label))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Test rule", "test", List.of()));
    }

    /** Snapshot fixture that records how often the persisted messages are read. */
    private static final class CountingSnapshotStore implements MessageSnapshotStore {
        private final List<MailMessage> messages;
        private int loadCount;

        private CountingSnapshotStore(MailMessage message) {
            this.messages = List.of(message);
        }

        @Override
        public void write(List<MailMessage> written) {
            throw new UnsupportedOperationException("dry-run must not write the snapshot");
        }

        @Override
        public List<MailMessage> load() {
            loadCount++;
            return messages;
        }
    }

    private static final class CountingGateway implements MailboxGateway {
        private int readCount;

        @Override
        public List<MailboxLabel> listLabels() {
            readCount++;
            return List.of(
                    new MailboxLabel(LABEL_ID, LABEL_NAME),
                    new MailboxLabel("Label_2", "Newsletters/Google"));
        }

        @Override
        public List<MessageId> listMessageIds(String query) {
            return List.of();
        }
    }
}
