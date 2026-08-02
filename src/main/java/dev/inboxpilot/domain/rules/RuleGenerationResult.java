package dev.inboxpilot.domain.rules;

import java.util.List;

/** Generated rules together with deterministic safety accounting. */
public record RuleGenerationResult(
        RuleSet rules,
        List<RejectedRuleSuggestion> rejectedSuggestions,
        int generatedSuggestions,
        int deduplicatedSuggestions) {

    public RuleGenerationResult {
        rejectedSuggestions = List.copyOf(rejectedSuggestions);
    }

    public int finalSuggestions() {
        return rules.rules().size();
    }
}
