package dev.inboxpilot.domain.ai;

/** AI suggestion paired with an explicit human decision. */
public record ReviewedAiSuggestion(
        AiClassificationSuggestion suggestion,
        AiHumanDecision decision) {

    public boolean accepted() {
        return decision == AiHumanDecision.ACCEPT;
    }
}
