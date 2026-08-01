package dev.inboxpilot.application.classification;

import dev.inboxpilot.application.model.LabelBatchChange;
import dev.inboxpilot.application.port.LabelChangeGateway;
import dev.inboxpilot.application.port.RollbackPlanStore;
import dev.inboxpilot.domain.classification.ClassificationAssessment;
import dev.inboxpilot.domain.classification.ClassificationPlan;
import dev.inboxpilot.domain.classification.MessageLabelPlan;
import dev.inboxpilot.domain.classification.RollbackPlan;
import dev.inboxpilot.domain.classification.RollbackPlanGenerator;
import java.util.List;

/** Explicit execution use case that persists rollback before applying clean plans. */
public final class ClassificationExecutor {

    private final RollbackPlanStore rollbackStore;
    private final LabelChangeGateway labelGateway;
    private final RollbackPlanGenerator rollbackGenerator = new RollbackPlanGenerator();

    public ClassificationExecutor(
            RollbackPlanStore rollbackStore, LabelChangeGateway labelGateway) {
        this.rollbackStore = rollbackStore;
        this.labelGateway = labelGateway;
    }

    public void execute(ClassificationAssessment assessment) {
        List<MessageLabelPlan> changed = assessment.clean().stream()
                .filter(MessageLabelPlan::changesLabels)
                .toList();
        RollbackPlan rollback = rollbackGenerator.generate(new ClassificationPlan(changed));
        rollbackStore.save(rollback);
        changed.forEach(this::apply);
    }

    private void apply(MessageLabelPlan plan) {
        labelGateway.apply(new LabelBatchChange(
                List.of(plan.messageId()), plan.addLabels(), plan.removeLabels()));
    }
}
