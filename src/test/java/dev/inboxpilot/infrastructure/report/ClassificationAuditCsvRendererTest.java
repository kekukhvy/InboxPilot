package dev.inboxpilot.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.classification.ClassificationAuditEntry;
import dev.inboxpilot.domain.classification.ClassificationAuditReport;
import dev.inboxpilot.domain.classification.ClassificationAuditResult;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClassificationAuditCsvRenderer")
class ClassificationAuditCsvRendererTest {

    @Test
    @DisplayName("exports deterministic escaped audit rows")
    void exportsAuditRows() {
        ClassificationAuditEntry entry = new ClassificationAuditEntry(
                new RuleId("rule"), new MessageId("message"), List.of("Label_Old"),
                List.of("Label_New"), List.of("Label_New"), ClassificationAuditResult.APPLIED);

        String csv = new ClassificationAuditCsvRenderer()
                .render(new ClassificationAuditReport(List.of(entry)));

        assertThat(csv).startsWith("ruleId,messageId").contains(
                "\"rule\",\"message\",\"Label_Old\",\"Label_New\",\"Label_New\",\"APPLIED\"");
    }
}
