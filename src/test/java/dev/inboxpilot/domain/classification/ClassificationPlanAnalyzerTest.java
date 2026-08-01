package dev.inboxpilot.domain.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.MessageId;
import dev.inboxpilot.domain.rules.RuleId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClassificationPlanAnalyzer")
class ClassificationPlanAnalyzerTest {

    @Test
    @DisplayName("separates clean, ambiguous, and unmatched messages")
    void separatesOutcomes() {
        MessageLabelPlan clean = plan("clean", List.of(new RuleId("one")));
        MessageLabelPlan ambiguous = plan(
                "ambiguous", List.of(new RuleId("one"), new RuleId("two")));
        MessageLabelPlan unmatched = plan("unmatched", List.of());

        ClassificationAssessment assessment = new ClassificationPlanAnalyzer()
                .analyze(new ClassificationPlan(List.of(clean, ambiguous, unmatched)));

        assertThat(assessment.clean()).containsExactly(clean);
        assertThat(assessment.ambiguous()).containsExactly(ambiguous);
        assertThat(assessment.unmatched()).containsExactly(unmatched);
    }

    private static MessageLabelPlan plan(String id, List<RuleId> rules) {
        return new MessageLabelPlan(
                new MessageId(id), rules, List.of("INBOX"),
                List.of(), List.of(), List.of("INBOX"));
    }
}
