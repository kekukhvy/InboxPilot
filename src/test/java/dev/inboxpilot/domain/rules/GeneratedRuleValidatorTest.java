package dev.inboxpilot.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GeneratedRuleValidatorTest {

    private static final String EXCLUDED_DOMAIN = "privaterelay.appleid.com";
    private static final RuleGenerationPolicy POLICY = new RuleGenerationPolicy(
            Set.of(EXCLUDED_DOMAIN), Map.of(), Set.of(), 20);

    @Test
    void rejectsProviderLabelIds() {
        RuleDefinition rule = rule(
                "provider-id", "sender-domain", "example.com", "Label_12345");

        assertThat(new GeneratedRuleValidator().validate(List.of(rule), POLICY))
                .extracting(RuleValidationFinding::errorCode)
                .contains("provider-label-id");
    }

    @Test
    void rejectsDuplicateRuleIds() {
        RuleDefinition first = rule("duplicate", "sender-domain", "one.example", "One");
        RuleDefinition second = rule("duplicate", "sender-domain", "two.example", "Two");

        assertThat(new GeneratedRuleValidator().validate(List.of(first, second), POLICY))
                .extracting(RuleValidationFinding::errorCode)
                .contains("duplicate-rule-id");
    }

    @Test
    void rejectsExcludedDomainRules() {
        RuleDefinition rule = rule(
                "excluded", "sender-domain", EXCLUDED_DOMAIN, "Accounts/Apple");

        assertThat(new GeneratedRuleValidator().validate(List.of(rule), POLICY))
                .extracting(RuleValidationFinding::errorCode)
                .contains("excluded-domain");
    }

    private static RuleDefinition rule(
            String id, String operator, String value, String label) {
        return new RuleDefinition(
                new RuleId(id), 100,
                new RuleConditionSpec(
                        operator, Map.of("value", value, "ignore-case", "true"), List.of()),
                List.of(new RuleActionSpec("add-label", Map.of("label", label))),
                RuleConflictBehavior.CONTINUE,
                new RuleMetadata("Test", "test", List.of()));
    }
}
