package dev.inboxpilot.domain.rules;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.List;
import java.util.Set;

/** Removes only provably redundant exact-sender suggestions. */
public final class RuleSuggestionDeduplicator {

    private static final String SENDER_OPERATOR = "sender-exact";
    private static final String DOMAIN_OPERATOR = "sender-domain";
    private static final String VALUE_PARAMETER = "value";

    public List<RuleDefinition> deduplicate(
            List<RuleDefinition> senderRules,
            List<RuleDefinition> domainRules,
            Set<String> senderAllowlist) {
        return senderRules.stream()
                .filter(sender -> keep(sender, domainRules, senderAllowlist))
                .toList();
    }

    private static boolean keep(
            RuleDefinition sender,
            List<RuleDefinition> domainRules,
            Set<String> senderAllowlist) {
        String senderAddress = conditionValue(sender);
        return senderAddress == null || senderAllowlist.contains(senderAddress)
                || !isRedundantExactSender(sender, domainRules);
    }

    private static boolean isRedundantExactSender(
            RuleDefinition sender, List<RuleDefinition> domainRules) {
        if (!isPlainExactSender(sender.condition())) {
            return false;
        }
        String senderDomain = new EmailAddress(conditionValue(sender)).domain();
        return domainRules.stream()
                .filter(domain -> DOMAIN_OPERATOR.equals(domain.condition().operator()))
                .anyMatch(domain -> conditionValue(domain).equals(senderDomain)
                        && domain.actions().equals(sender.actions()));
    }

    private static boolean isPlainExactSender(RuleConditionSpec condition) {
        return SENDER_OPERATOR.equals(condition.operator()) && condition.operands().isEmpty();
    }

    private static String conditionValue(RuleDefinition rule) {
        return rule.condition().parameters().get(VALUE_PARAMETER);
    }
}
