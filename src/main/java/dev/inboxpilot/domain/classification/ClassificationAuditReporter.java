package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MessageId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Joins dry-run plans with execution observations into reviewable audit rows. */
public final class ClassificationAuditReporter {

    public ClassificationAuditReport create(
            ClassificationPlan plan,
            Map<MessageId, ClassificationObservation> observations) {
        List<ClassificationAuditEntry> entries = plan.messages().stream()
                .flatMap(message -> entries(message, observations.get(message.messageId())))
                .sorted(Comparator.comparing((ClassificationAuditEntry entry) -> entry.messageId().value())
                        .thenComparing(entry -> entry.ruleId().value()))
                .toList();
        return new ClassificationAuditReport(entries);
    }

    private static Stream<ClassificationAuditEntry> entries(
            MessageLabelPlan plan, ClassificationObservation observation) {
        ClassificationAuditResult result = observation == null
                ? ClassificationAuditResult.PLANNED
                : observation.result();
        List<String> newLabels = observation == null ? plan.oldLabels() : observation.newLabels();
        return plan.matchedRuleIds().stream().map(ruleId -> new ClassificationAuditEntry(
                ruleId, plan.messageId(), plan.oldLabels(), plan.newLabels(), newLabels, result));
    }
}
