package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleSuggestionDeduplicatorTest {

    @Test
    void removesPlainSenderWithTheSameDomainAction() {
        assertThat(deduplicate(senderCondition(), "Same"))
                .extracting(rule -> rule.id().value()).containsExactly("domain");
    }

    @Test
    void keepsSenderWithADifferentAction() {
        assertThat(deduplicate(senderCondition(), "Different"))
                .extracting(rule -> rule.id().value()).containsExactly("domain", "sender");
    }

    @Test
    void keepsSenderWithAdditionalSubjectCondition() {
        RuleConditionSpec composite = new RuleConditionSpec("all", Map.of(), List.of(
                senderCondition(),
                new RuleConditionSpec("subject-contains", Map.of("value", "Security"), List.of())));

        assertThat(deduplicate(composite, "Same"))
                .extracting(rule -> rule.id().value()).containsExactly("domain", "sender");
    }

    private static List<RuleDefinition> deduplicate(
            RuleConditionSpec senderCondition, String senderLabel) {
        RuleDefinition domain = rule("domain",
                new RuleConditionSpec("sender-domain", Map.of("value", "example.com"), List.of()),
                "Same");
        RuleDefinition sender = rule("sender", senderCondition, senderLabel);
        List<RuleDefinition> senders = new RuleSuggestionDeduplicator()
                .deduplicate(List.of(sender), List.of(domain), Set.of());
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(domain), senders.stream()).toList();
    }

    private static RuleConditionSpec senderCondition() {
        return new RuleConditionSpec(
                "sender-exact", Map.of("value", "news@example.com"), List.of());
    }

    private static RuleDefinition rule(
            String id, RuleConditionSpec condition, String label) {
        return new RuleDefinition(new RuleId(id), 100, condition,
                List.of(new RuleActionSpec("add-label", Map.of("label", label))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Test", "test", List.of()));
    }
}
