package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RuleActionPlanner")
class RuleActionPlannerTest {

    @Test
    @DisplayName("plans ordered add-label and remove-label actions with rule attribution")
    void plansSupportedActions() {
        RuleMatch first = match("first",
                action("add-label", "Newsletters"), action("remove-label", "INBOX"));
        RuleMatch second = match("second", action("add-label", "Newsletters"));

        LabelChangePlan plan = new RuleActionPlanner()
                .plan(new RuleEvaluation(List.of(first, second), false));

        assertThat(plan.changes()).containsExactly(
                new PlannedLabelChange(new RuleId("first"), LabelChangeType.ADD, "Newsletters"),
                new PlannedLabelChange(new RuleId("first"), LabelChangeType.REMOVE, "INBOX"),
                new PlannedLabelChange(new RuleId("second"), LabelChangeType.ADD, "Newsletters"));
    }

    @Test
    @DisplayName("rejects destructive and unknown actions")
    void rejectsDestructiveActions() {
        RuleEvaluation evaluation = new RuleEvaluation(
                List.of(match("unsafe", action("delete-message", "ignored"))), false);

        assertThatThrownBy(() -> new RuleActionPlanner().plan(evaluation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported rule action");
    }

    private static RuleMatch match(String id, RuleActionSpec... actions) {
        return new RuleMatch(new RuleId(id), 1, List.of(actions));
    }

    private static RuleActionSpec action(String type, String label) {
        return new RuleActionSpec(type, Map.of("label", label));
    }
}
