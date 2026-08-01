package dev.inboxpilot.domain.rules;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Versioned persisted rule document. */
public record RuleSet(int version, List<RuleDefinition> rules) {

    public static final int CURRENT_VERSION = 1;

    public RuleSet {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported rule schema version: " + version);
        }
        rules = List.copyOf(rules);
        Set<RuleId> uniqueIds = rules.stream().map(RuleDefinition::id).collect(Collectors.toSet());
        if (uniqueIds.size() != rules.size()) {
            throw new IllegalArgumentException("Rule ids must be unique");
        }
    }
}
