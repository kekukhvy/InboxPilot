package dev.inboxpilot.domain.rules;

import java.util.List;
import java.util.Objects;

/** Complete provider-independent rule represented by one YAML rule entry. */
public record RuleDefinition(
        RuleId id,
        int priority,
        RuleConditionSpec condition,
        List<RuleActionSpec> actions,
        RuleConflictBehavior conflictBehavior,
        RuleMetadata metadata) {

    public RuleDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(conflictBehavior, "conflictBehavior");
        Objects.requireNonNull(metadata, "metadata");
        actions = List.copyOf(actions);
        if (priority < 0) {
            throw new IllegalArgumentException("Rule priority must not be negative");
        }
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("Rule must define at least one action");
        }
    }
}
