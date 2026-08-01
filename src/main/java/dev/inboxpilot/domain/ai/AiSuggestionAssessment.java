package dev.inboxpilot.domain.ai;

import java.util.List;

/** Disjoint automatic-eligibility and mandatory human-review queues. */
public record AiSuggestionAssessment(
        List<AiClassificationSuggestion> autoEligible,
        List<AiClassificationSuggestion> humanReviewRequired) {

    public AiSuggestionAssessment {
        autoEligible = List.copyOf(autoEligible);
        humanReviewRequired = List.copyOf(humanReviewRequired);
    }
}
