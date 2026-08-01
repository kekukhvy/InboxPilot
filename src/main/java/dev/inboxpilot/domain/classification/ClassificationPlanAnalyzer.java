package dev.inboxpilot.domain.classification;

import java.util.List;

/** Separates unambiguous plans from conflicts and unmatched messages. */
public final class ClassificationPlanAnalyzer {

    private static final int SINGLE_MATCH = 1;

    public ClassificationAssessment analyze(ClassificationPlan plan) {
        List<MessageLabelPlan> clean = plan.messages().stream()
                .filter(message -> message.matchedRuleIds().size() == SINGLE_MATCH)
                .toList();
        List<MessageLabelPlan> ambiguous = plan.messages().stream()
                .filter(message -> message.matchedRuleIds().size() > SINGLE_MATCH)
                .toList();
        List<MessageLabelPlan> unmatched = plan.messages().stream()
                .filter(message -> message.matchedRuleIds().isEmpty())
                .toList();
        return new ClassificationAssessment(clean, ambiguous, unmatched);
    }
}
