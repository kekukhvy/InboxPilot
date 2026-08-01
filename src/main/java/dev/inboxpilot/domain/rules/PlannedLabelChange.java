package dev.inboxpilot.domain.rules;

import java.util.Objects;

/** One label change attributed to the rule that proposed it. */
public record PlannedLabelChange(RuleId ruleId, LabelChangeType type, String label) {

    public PlannedLabelChange {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(label, "label");
        if (label.isBlank()) {
            throw new IllegalArgumentException("Label action requires a non-blank label");
        }
    }
}
