package dev.inboxpilot.domain.rules;

import dev.inboxpilot.domain.message.MailMessage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Evaluates matching rules in deterministic priority order. */
public final class RuleEvaluator {

    private static final Comparator<RuleDefinition> EVALUATION_ORDER = RuleSpecificity.order();
    private final ConditionCompiler conditionCompiler = new ConditionCompiler();

    public RuleEvaluation evaluate(RuleSet ruleSet, MailMessage message) {
        List<RuleMatch> matches = new ArrayList<>();
        List<RuleDefinition> orderedRules = ruleSet.rules().stream().sorted(EVALUATION_ORDER).toList();
        for (RuleDefinition rule : orderedRules) {
            if (!conditionCompiler.compile(rule.condition()).matches(message)) {
                continue;
            }
            matches.add(new RuleMatch(rule.id(), rule.priority(), rule.actions()));
            if (rule.conflictBehavior() == RuleConflictBehavior.STOP) {
                return new RuleEvaluation(matches, true);
            }
        }
        return new RuleEvaluation(matches, false);
    }
}
