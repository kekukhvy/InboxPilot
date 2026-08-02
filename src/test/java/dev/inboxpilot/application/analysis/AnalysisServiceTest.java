package dev.inboxpilot.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.port.InventorySnapshotStore;
import dev.inboxpilot.application.model.MailboxLabel;
import dev.inboxpilot.application.port.MailboxGateway;
import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisServiceTest {

    private static final String USER_LABEL = "Label_work";
    private static final String USER_LABEL_NAME = "Work/Projects";
    private static final String SYSTEM_LABEL = "INBOX";
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-02T12:00:00Z");
    private static final int CLASSIFIED_MESSAGES = 30;
    private static final int UNCLASSIFIED_MESSAGES = 3;
    private static final String CLASSIFIED_SENDER = "work@example.com";
    private static final String UNCLASSIFIED_SENDER = "news@example.org";
    private static final Path SUMMARY_PATH = Path.of("reports/summary.json");

    @Test
    void analyzesOnlyThePersistedInventorySnapshot() {
        InventorySnapshotStore store = () -> inventory();
        java.util.concurrent.atomic.AtomicReference<AnalysisReport> captured =
                new java.util.concurrent.atomic.AtomicReference<>();

        AnalysisRunResult result = new AnalysisService(
                store, report -> {
                    captured.set(report);
                    return List.of(SUMMARY_PATH);
                }, gateway()).run();

        assertThat(result).isEqualTo(new AnalysisRunResult(
                CLASSIFIED_MESSAGES + UNCLASSIFIED_MESSAGES, 1, 1,
                List.of(SUMMARY_PATH)));
        assertThat(captured.get().ruleSuggestions().rules())
                .allSatisfy(rule -> assertThat(rule.actions().getFirst().parameters())
                        .containsEntry("label", USER_LABEL_NAME));
    }

    private static MailboxGateway gateway() {
        return new MailboxGateway() {
            @Override
            public List<MailboxLabel> listLabels() {
                return List.of(
                        new MailboxLabel(USER_LABEL, USER_LABEL_NAME),
                        new MailboxLabel(SYSTEM_LABEL, SYSTEM_LABEL));
            }

            @Override
            public List<MessageId> listMessageIds(String query) {
                return List.of();
            }
        };
    }

    private static Inventory inventory() {
        return new Inventory(
                List.of(
                        sender(CLASSIFIED_SENDER, CLASSIFIED_MESSAGES, List.of(USER_LABEL)),
                        sender(UNCLASSIFIED_SENDER, UNCLASSIFIED_MESSAGES, List.of(SYSTEM_LABEL))),
                List.of(
                        domain("example.com", CLASSIFIED_MESSAGES, List.of(USER_LABEL)),
                        domain("example.org", UNCLASSIFIED_MESSAGES, List.of(SYSTEM_LABEL))));
    }

    private static SenderInventory sender(String value, int count, List<String> labels) {
        return new SenderInventory(new EmailAddress(value), statistics(count, labels));
    }

    private static DomainInventory domain(String value, int count, List<String> labels) {
        return new DomainInventory(value, statistics(count, labels));
    }

    private static InventoryStatistics statistics(int count, List<String> labels) {
        return new InventoryStatistics(
                count, 0, RECEIVED_AT, RECEIVED_AT, labels, List.of());
    }
}
