package dev.inboxpilot.domain.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClassificationVerificationScanner")
class ClassificationVerificationScannerTest {

    @Test
    @DisplayName("reports matches, label discrepancies, and missing messages")
    void reportsVerificationOutcomes() {
        ClassificationPlan plan = new ClassificationPlan(List.of(
                plan("matched", List.of("Label_New")),
                plan("mismatch", List.of("Label_New")),
                plan("missing", List.of("Label_New"))));
        List<MailMessage> freshMessages = List.of(
                message("matched", List.of("Label_New")),
                message("mismatch", List.of("Label_Other")));

        ClassificationVerificationReport report = new ClassificationVerificationScanner()
                .verify(plan, freshMessages);

        assertThat(report.successful()).isFalse();
        assertThat(report.entries()).extracting(ClassificationVerificationEntry::status)
                .containsExactly(
                        ClassificationVerificationStatus.MATCH,
                        ClassificationVerificationStatus.LABEL_MISMATCH,
                        ClassificationVerificationStatus.MESSAGE_MISSING);
        assertThat(report.entries().get(1).missingLabels()).containsExactly("Label_New");
        assertThat(report.entries().get(1).unexpectedLabels()).containsExactly("Label_Other");
    }

    private static MessageLabelPlan plan(String id, List<String> expectedLabels) {
        return new MessageLabelPlan(
                new MessageId(id), List.of(new RuleId("rule")), List.of(),
                expectedLabels, List.of(), expectedLabels);
    }

    private static MailMessage message(String id, List<String> labels) {
        return new MailMessage(
                new MessageId(id), new EmailAddress("sender@example.com"), "Subject",
                Instant.parse("2026-01-01T00:00:00Z"), labels);
    }
}
