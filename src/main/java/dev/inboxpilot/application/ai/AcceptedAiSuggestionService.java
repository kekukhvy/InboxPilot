package dev.inboxpilot.application.ai;

import dev.inboxpilot.application.port.AcceptedRuleStore;
import dev.inboxpilot.domain.ai.AcceptedAiRuleConverter;
import dev.inboxpilot.domain.ai.ReviewedAiSuggestion;
import dev.inboxpilot.domain.rules.RuleDefinition;
import dev.inboxpilot.domain.rules.RuleSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Persists only human-accepted AI suggestions as deterministic rules. */
public final class AcceptedAiSuggestionService {

    private final AcceptedRuleStore store;
    private final AcceptedAiRuleConverter converter = new AcceptedAiRuleConverter();

    public AcceptedAiSuggestionService(AcceptedRuleStore store) {
        this.store = store;
    }

    public RuleSet persist(Collection<ReviewedAiSuggestion> reviewedSuggestions) {
        List<RuleDefinition> rules = reviewedSuggestions.stream()
                .filter(ReviewedAiSuggestion::accepted)
                .map(converter::convert)
                .collect(java.util.stream.Collectors.toMap(
                        RuleDefinition::id, rule -> rule, (first, ignored) -> first))
                .values().stream()
                .sorted(Comparator.comparing(rule -> rule.id().value()))
                .toList();
        RuleSet ruleSet = new RuleSet(RuleSet.CURRENT_VERSION, rules);
        store.save(ruleSet);
        return ruleSet;
    }
}
