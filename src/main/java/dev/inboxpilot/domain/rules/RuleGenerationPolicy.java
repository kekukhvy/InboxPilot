package dev.inboxpilot.domain.rules;

import java.util.Map;
import java.util.Set;

/** Configurable safety boundaries for automatic starter-rule suggestions. */
public record RuleGenerationPolicy(
        Set<String> excludedDomains,
        Map<String, String> knownDomainMappings,
        Set<String> senderRuleAllowlist,
        int minimumMessageCount) {

    public RuleGenerationPolicy {
        excludedDomains = Set.copyOf(excludedDomains);
        knownDomainMappings = Map.copyOf(knownDomainMappings);
        senderRuleAllowlist = Set.copyOf(senderRuleAllowlist);
        if (minimumMessageCount < 1) {
            throw new IllegalArgumentException("Minimum message count must be positive");
        }
    }
}
