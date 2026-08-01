package dev.inboxpilot.domain.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClassificationAuditReporter")
class ClassificationAuditReporterTest {

    @Test
    @DisplayName("records rule, message, old, planned, actual labels, and result")
    void createsCompleteAuditEntries() {
        MessageId messageId = new MessageId("m1");
        MessageLabelPlan message = new MessageLabelPlan(
                messageId, List.of(new RuleId("rule-one")),
                List.of("Label_Old"), List.of("Label_New"), List.of("Label_Old"), List.of("Label_New"));
        ClassificationObservation observation = new ClassificationObservation(
                ClassificationAuditResult.APPLIED, List.of("Label_New"));

        ClassificationAuditReport report = new ClassificationAuditReporter()
                .create(new ClassificationPlan(List.of(message)), Map.of(messageId, observation));

        assertThat(report.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.ruleId()).isEqualTo(new RuleId("rule-one"));
            assertThat(entry.messageId()).isEqualTo(messageId);
            assertThat(entry.oldLabels()).containsExactly("Label_Old");
            assertThat(entry.plannedLabels()).containsExactly("Label_New");
            assertThat(entry.newLabels()).containsExactly("Label_New");
            assertThat(entry.result()).isEqualTo(ClassificationAuditResult.APPLIED);
        });
    }
}
