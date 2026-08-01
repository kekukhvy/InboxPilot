package dev.inboxpilot.domain.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RollbackPlanGenerator")
class RollbackPlanGeneratorTest {

    @Test
    @DisplayName("exports inverse operations and omits no-op messages")
    void generatesInverseChanges() {
        MessageLabelPlan changed = new MessageLabelPlan(
                new MessageId("changed"), List.of(new RuleId("rule")), List.of("Label_Old"),
                List.of("Label_New"), List.of("Label_Old"), List.of("Label_New"));
        MessageLabelPlan unchanged = new MessageLabelPlan(
                new MessageId("unchanged"), List.of(new RuleId("rule")), List.of("Label_Keep"),
                List.of(), List.of(), List.of("Label_Keep"));

        RollbackPlan rollback = new RollbackPlanGenerator()
                .generate(new ClassificationPlan(List.of(changed, unchanged)));

        assertThat(rollback.changes()).singleElement().satisfies(change -> {
            assertThat(change.messageId()).isEqualTo(new MessageId("changed"));
            assertThat(change.addLabelIds()).containsExactly("Label_Old");
            assertThat(change.removeLabelIds()).containsExactly("Label_New");
        });
    }
}
