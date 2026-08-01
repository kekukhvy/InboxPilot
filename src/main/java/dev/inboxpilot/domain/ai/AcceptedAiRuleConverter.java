package dev.inboxpilot.domain.ai;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.rules.RuleActionSpec;
import dev.inboxpilot.domain.rules.RuleConditionSpec;
import dev.inboxpilot.domain.rules.RuleConflictBehavior;
import dev.inboxpilot.domain.rules.RuleDefinition;
import dev.inboxpilot.domain.rules.RuleId;
import dev.inboxpilot.domain.rules.RuleMetadata;
import dev.inboxpilot.domain.rules.RuleTestCase;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Converts an explicitly accepted AI suggestion into a deterministic rule. */
public final class AcceptedAiRuleConverter {

    private static final int RULE_PRIORITY = 50;
    private static final int HASH_RADIX = 36;

    public RuleDefinition convert(ReviewedAiSuggestion reviewed) {
        if (!reviewed.accepted()) {
            throw new IllegalArgumentException("Only accepted AI suggestions can become rules");
        }
        AiClassificationSuggestion suggestion = reviewed.suggestion();
        String id = "ai-domain-" + slug(suggestion.senderDomain()) + "-"
                + Integer.toUnsignedString(suggestion.senderDomain().hashCode(), HASH_RADIX);
        RuleConditionSpec condition = new RuleConditionSpec(
                "sender-domain",
                Map.of("value", suggestion.senderDomain(), "ignore-case", "true"),
                List.of());
        RuleActionSpec action = new RuleActionSpec(
                "add-label", Map.of("label", suggestion.proposedLabel()));
        RuleMetadata metadata = new RuleMetadata(
                suggestion.rationale(), "ai-confirmed", List.of("ai-confirmed"));
        RuleTestCase test = new RuleTestCase(
                "matches confirmed sender domain",
                new EmailAddress("example@" + suggestion.senderDomain()), "Confirmed AI example", true);
        return new RuleDefinition(new RuleId(id), RULE_PRIORITY, condition, List.of(action),
                RuleConflictBehavior.CONTINUE, metadata, List.of(test));
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
