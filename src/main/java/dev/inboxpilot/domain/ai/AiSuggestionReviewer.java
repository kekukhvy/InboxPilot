package dev.inboxpilot.domain.ai;

import java.util.Collection;
import java.util.List;

/** Enforces the confidence threshold and records explicit human review. */
public final class AiSuggestionReviewer {

    private final double minimumAutomaticConfidence;

    public AiSuggestionReviewer(double minimumAutomaticConfidence) {
        if (!Double.isFinite(minimumAutomaticConfidence)
                || minimumAutomaticConfidence < 0.0
                || minimumAutomaticConfidence > 1.0) {
            throw new IllegalArgumentException("AI confidence threshold must be between 0 and 1");
        }
        this.minimumAutomaticConfidence = minimumAutomaticConfidence;
    }

    public AiSuggestionAssessment assess(
            Collection<AiClassificationSuggestion> suggestions) {
        List<AiClassificationSuggestion> automatic = suggestions.stream()
                .filter(this::meetsThreshold)
                .toList();
        List<AiClassificationSuggestion> review = suggestions.stream()
                .filter(suggestion -> !meetsThreshold(suggestion))
                .toList();
        return new AiSuggestionAssessment(automatic, review);
    }

    public ReviewedAiSuggestion review(
            AiClassificationSuggestion suggestion, AiHumanDecision decision) {
        return new ReviewedAiSuggestion(suggestion, decision);
    }

    private boolean meetsThreshold(AiClassificationSuggestion suggestion) {
        return suggestion.confidence() >= minimumAutomaticConfidence;
    }
}
