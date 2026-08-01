package dev.inboxpilot.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiSuggestionReviewer")
class AiSuggestionReviewerTest {

    @Test
    @DisplayName("never marks a below-threshold suggestion automatically eligible")
    void routesLowConfidenceToHumanReview() {
        AiClassificationSuggestion low = suggestion(0.79);
        AiClassificationSuggestion high = suggestion(0.80);

        AiSuggestionAssessment assessment = new AiSuggestionReviewer(0.80)
                .assess(List.of(low, high));

        assertThat(assessment.autoEligible()).containsExactly(high).doesNotContain(low);
        assertThat(assessment.humanReviewRequired()).containsExactly(low);
    }

    @Test
    @DisplayName("records an explicit human decision for a reviewed suggestion")
    void recordsHumanDecision() {
        ReviewedAiSuggestion reviewed = new AiSuggestionReviewer(0.90)
                .review(suggestion(0.20), AiHumanDecision.ACCEPT);

        assertThat(reviewed.accepted()).isTrue();
        assertThat(reviewed.decision()).isEqualTo(AiHumanDecision.ACCEPT);
    }

    private static AiClassificationSuggestion suggestion(double confidence) {
        return new AiClassificationSuggestion(
                "example.com", "Newsletters", confidence, "Classifier rationale");
    }
}
