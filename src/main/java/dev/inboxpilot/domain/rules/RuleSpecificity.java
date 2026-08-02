package dev.inboxpilot.domain.rules;

import java.util.Comparator;

/** Computes deterministic evaluation order for equally prioritized rules. */
public final class RuleSpecificity {

    private static final int EXACT_SENDER_SCORE = 300;
    private static final int SUBDOMAIN_SCORE = 200;
    private static final int ROOT_DOMAIN_SCORE = 100;
    private static final String SENDER_EXACT = "sender-exact";
    private static final String SENDER_DOMAIN = "sender-domain";

    private RuleSpecificity() {
    }

    public static Comparator<RuleDefinition> order() {
        return Comparator.comparingInt(RuleDefinition::priority).reversed()
                .thenComparing(Comparator.comparingInt(RuleSpecificity::score).reversed())
                .thenComparing(rule -> rule.id().value());
    }

    public static int score(RuleDefinition rule) {
        return conditionScore(rule.condition());
    }

    private static int conditionScore(RuleConditionSpec condition) {
        String operator = condition.operator();
        if (SENDER_EXACT.equals(operator)) {
            return EXACT_SENDER_SCORE;
        }
        if (SENDER_DOMAIN.equals(operator)) {
            String domain = condition.parameters().getOrDefault("value", "");
            return domain.chars().filter(character -> character == '.').count() > 1
                    ? SUBDOMAIN_SCORE : ROOT_DOMAIN_SCORE;
        }
        int childScore = condition.operands().stream()
                .mapToInt(RuleSpecificity::conditionScore).max().orElse(0);
        return childScore + condition.operands().size();
    }
}
