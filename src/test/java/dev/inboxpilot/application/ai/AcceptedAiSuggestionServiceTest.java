package dev.inboxpilot.application.ai;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.port.AcceptedRuleStore;
import dev.inboxpilot.domain.ai.AiClassificationSuggestion;
import dev.inboxpilot.domain.ai.AiHumanDecision;
import dev.inboxpilot.domain.ai.ReviewedAiSuggestion;
import dev.inboxpilot.domain.rules.RuleSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AcceptedAiSuggestionService")
class AcceptedAiSuggestionServiceTest {

    @Test
    @DisplayName("persists accepted suggestions and excludes rejected suggestions")
    void persistsAcceptedSuggestions() {
        RecordingStore store = new RecordingStore();
        ReviewedAiSuggestion accepted = reviewed("example.com", AiHumanDecision.ACCEPT);
        ReviewedAiSuggestion rejected = reviewed("other.com", AiHumanDecision.REJECT);

        RuleSet rules = new AcceptedAiSuggestionService(store)
                .persist(List.of(accepted, rejected));

        assertThat(rules.rules()).singleElement().satisfies(rule -> {
            assertThat(rule.condition().parameters()).containsEntry("value", "example.com");
            assertThat(rule.actions().getFirst().parameters()).containsEntry("label", "Label_News");
            assertThat(rule.metadata().source()).isEqualTo("ai-confirmed");
        });
        assertThat(store.saved).isSameAs(rules);
    }

    private static ReviewedAiSuggestion reviewed(String domain, AiHumanDecision decision) {
        AiClassificationSuggestion suggestion = new AiClassificationSuggestion(
                domain, "Label_News", 0.5, "Confirmed rationale");
        return new ReviewedAiSuggestion(suggestion, decision);
    }

    private static final class RecordingStore implements AcceptedRuleStore {
        private RuleSet saved;

        @Override
        public void save(RuleSet ruleSet) {
            saved = ruleSet;
        }
    }
}
