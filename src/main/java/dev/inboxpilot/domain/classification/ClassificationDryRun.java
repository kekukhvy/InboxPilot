package dev.inboxpilot.domain.classification;

import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.rules.LabelChangePlan;
import dev.inboxpilot.domain.rules.LabelChangeType;
import dev.inboxpilot.domain.rules.RuleActionPlanner;
import dev.inboxpilot.domain.rules.RuleEvaluation;
import dev.inboxpilot.domain.rules.RuleEvaluator;
import dev.inboxpilot.domain.rules.RuleId;
import dev.inboxpilot.domain.rules.RuleSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Evaluates rules and computes label diffs without invoking any mailbox port. */
public final class ClassificationDryRun {

    private final RuleEvaluator evaluator = new RuleEvaluator();
    private final RuleActionPlanner actionPlanner = new RuleActionPlanner();

    public ClassificationPlan plan(RuleSet rules, Collection<MailMessage> messages) {
        List<MessageLabelPlan> plans = messages.stream()
                .map(message -> planMessage(rules, message))
                .toList();
        return new ClassificationPlan(plans);
    }

    private MessageLabelPlan planMessage(RuleSet rules, MailMessage message) {
        RuleEvaluation evaluation = evaluator.evaluate(rules, message);
        LabelChangePlan actions = actionPlanner.plan(evaluation);
        Set<String> oldLabels = new HashSet<>(message.labels());
        Set<String> desiredLabels = new LinkedHashSet<>(oldLabels);
        actions.changes().forEach(change -> {
            if (change.type() == LabelChangeType.ADD) {
                desiredLabels.add(change.label());
            } else {
                desiredLabels.remove(change.label());
            }
        });
        List<String> additions = difference(desiredLabels, oldLabels);
        List<String> removals = difference(oldLabels, desiredLabels);
        List<RuleId> ruleIds = evaluation.matches().stream().map(match -> match.id()).toList();
        return new MessageLabelPlan(message.id(), ruleIds, sorted(oldLabels),
                additions, removals, sorted(desiredLabels));
    }

    private static List<String> difference(Set<String> minuend, Set<String> subtrahend) {
        return minuend.stream().filter(label -> !subtrahend.contains(label)).sorted().toList();
    }

    private static List<String> sorted(Collection<String> values) {
        return values.stream().sorted().toList();
    }
}
