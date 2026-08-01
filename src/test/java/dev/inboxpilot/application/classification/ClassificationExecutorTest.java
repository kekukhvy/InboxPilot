package dev.inboxpilot.application.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.model.LabelBatchChange;
import dev.inboxpilot.application.port.LabelChangeGateway;
import dev.inboxpilot.application.port.RollbackPlanStore;
import dev.inboxpilot.domain.classification.ClassificationAssessment;
import dev.inboxpilot.domain.classification.MessageLabelPlan;
import dev.inboxpilot.domain.classification.RollbackPlan;
import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClassificationExecutor")
class ClassificationExecutorTest {

    @Test
    @DisplayName("persists rollback before applying clean label changes")
    void persistsRollbackFirst() {
        List<String> calls = new ArrayList<>();
        RecordingRollbackStore rollbackStore = new RecordingRollbackStore(calls);
        RecordingLabelGateway labelGateway = new RecordingLabelGateway(calls);
        MessageLabelPlan clean = new MessageLabelPlan(
                new MessageId("m1"), List.of(new RuleId("rule")), List.of("Label_Old"),
                List.of("Label_New"), List.of("Label_Old"), List.of("Label_New"));

        new ClassificationExecutor(rollbackStore, labelGateway)
                .execute(new ClassificationAssessment(List.of(clean), List.of(), List.of()));

        assertThat(calls).containsExactly("rollback", "gmail");
        assertThat(rollbackStore.plan.changes()).singleElement().satisfies(change -> {
            assertThat(change.addLabelIds()).containsExactly("Label_Old");
            assertThat(change.removeLabelIds()).containsExactly("Label_New");
        });
        assertThat(labelGateway.change.messageIds()).containsExactly(new MessageId("m1"));
    }

    private static final class RecordingRollbackStore implements RollbackPlanStore {
        private final List<String> calls;
        private RollbackPlan plan;

        private RecordingRollbackStore(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public void save(RollbackPlan savedPlan) {
            calls.add("rollback");
            plan = savedPlan;
        }
    }

    private static final class RecordingLabelGateway implements LabelChangeGateway {
        private final List<String> calls;
        private LabelBatchChange change;

        private RecordingLabelGateway(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public void apply(LabelBatchChange appliedChange) {
            calls.add("gmail");
            change = appliedChange;
        }
    }
}
