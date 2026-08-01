package dev.inboxpilot.domain.rules;

import java.util.List;

/** Deterministic matched-rule result for one message. */
public record RuleEvaluation(List<RuleMatch> matches, boolean stopped) {

    public RuleEvaluation {
        matches = List.copyOf(matches);
    }
}
