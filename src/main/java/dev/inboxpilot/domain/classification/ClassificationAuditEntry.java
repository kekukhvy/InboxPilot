package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.util.List;

/** Complete audit row attributing one message outcome to one matched rule. */
public record ClassificationAuditEntry(
        RuleId ruleId,
        MessageId messageId,
        List<String> oldLabels,
        List<String> plannedLabels,
        List<String> newLabels,
        ClassificationAuditResult result) {

    public ClassificationAuditEntry {
        oldLabels = List.copyOf(oldLabels);
        plannedLabels = List.copyOf(plannedLabels);
        newLabels = List.copyOf(newLabels);
    }
}
