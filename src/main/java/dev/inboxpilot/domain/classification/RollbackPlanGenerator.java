package dev.inboxpilot.domain.classification;

import java.util.List;

/** Reverses an exact classification diff without executing either direction. */
public final class RollbackPlanGenerator {

    public RollbackPlan generate(ClassificationPlan classificationPlan) {
        List<RollbackLabelChange> changes = classificationPlan.messages().stream()
                .filter(MessageLabelPlan::changesLabels)
                .map(plan -> new RollbackLabelChange(
                        plan.messageId(), plan.removeLabels(), plan.addLabels()))
                .toList();
        return new RollbackPlan(RollbackPlan.CURRENT_VERSION, changes);
    }
}
