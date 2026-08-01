package dev.inboxpilot.domain.rules;

import java.util.List;

/** Matched rule and its still-unexecuted action specifications. */
public record RuleMatch(RuleId id, int priority, List<RuleActionSpec> actions) {

    public RuleMatch {
        actions = List.copyOf(actions);
    }
}
