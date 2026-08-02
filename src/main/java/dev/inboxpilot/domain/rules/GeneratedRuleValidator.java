package dev.inboxpilot.domain.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rejects unsafe or contradictory generated rules before YAML persistence. */
public final class GeneratedRuleValidator {

    private static final int MAXIMUM_PRIORITY = 1_000;
    private static final String LABEL_PARAMETER = "label";
    private static final String VALUE_PARAMETER = "value";
    private static final String PROVIDER_LABEL_PREFIX = "Label_";
    private static final String DOMAIN_OPERATOR = "sender-domain";
    private static final Set<String> SUPPORTED_ACTIONS = Set.of("add-label", "remove-label");
    private final RuleTestRunner testRunner = new RuleTestRunner();

    public List<RuleValidationFinding> validate(
            List<RuleDefinition> rules, RuleGenerationPolicy policy) {
        List<RuleValidationFinding> findings = new ArrayList<>();
        validateIds(rules, findings);
        rules.forEach(rule -> validateRule(rule, policy, findings));
        validateEquivalentRules(rules, findings);
        validateSenderDomainDuplicates(rules, findings);
        return List.copyOf(findings);
    }

    private static void validateIds(
            List<RuleDefinition> rules, List<RuleValidationFinding> findings) {
        Set<RuleId> ids = new HashSet<>();
        rules.stream().filter(rule -> !ids.add(rule.id())).forEach(rule ->
                error(findings, rule, "duplicate-rule-id", "Rule id must be unique"));
    }

    private void validateRule(
            RuleDefinition rule,
            RuleGenerationPolicy policy,
            List<RuleValidationFinding> findings) {
        if (rule.priority() < 0 || rule.priority() > MAXIMUM_PRIORITY) {
            error(findings, rule, "invalid-priority", "Priority is outside the supported range");
        }
        if (rule.actions().isEmpty()) {
            error(findings, rule, "missing-action", "Rule must contain an action");
        }
        rule.actions().forEach(action -> validateAction(rule, action, findings));
        if (isExcludedDomainRule(rule, policy)) {
            error(findings, rule, "excluded-domain", "Domain is excluded from broad rules");
        }
        validateConditionAndTests(rule, findings);
    }

    private static void validateAction(
            RuleDefinition rule,
            RuleActionSpec action,
            List<RuleValidationFinding> findings) {
        if (!SUPPORTED_ACTIONS.contains(action.type())) {
            error(findings, rule, "unsupported-action", "Rule action is not supported");
        }
        String label = action.parameters().get(LABEL_PARAMETER);
        if (label == null || label.isBlank()) {
            error(findings, rule, "blank-label", "Label action must name a visible label");
        } else if (label.startsWith(PROVIDER_LABEL_PREFIX)) {
            error(findings, rule, "provider-label-id", "Rule labels must not expose Gmail IDs");
        }
    }

    private void validateConditionAndTests(
            RuleDefinition rule, List<RuleValidationFinding> findings) {
        try {
            var results = testRunner.run(rule).results();
            if (results.stream().anyMatch(result -> !result.passed())) {
                error(findings, rule, "failing-test-case",
                        "Embedded rule test does not match its expected result");
            }
        } catch (IllegalArgumentException exception) {
            error(findings, rule, "invalid-condition", exception.getMessage());
        }
    }

    private static boolean isExcludedDomainRule(
            RuleDefinition rule, RuleGenerationPolicy policy) {
        return DOMAIN_OPERATOR.equals(rule.condition().operator())
                && policy.excludedDomains().contains(
                        rule.condition().parameters().get(VALUE_PARAMETER));
    }

    private static void validateEquivalentRules(
            List<RuleDefinition> rules, List<RuleValidationFinding> findings) {
        Set<String> signatures = new HashSet<>();
        rules.stream().filter(rule -> !signatures.add(signature(rule))).forEach(rule ->
                error(findings, rule, "equivalent-rule", "Equivalent rule already exists"));
    }

    private static void validateSenderDomainDuplicates(
            List<RuleDefinition> rules, List<RuleValidationFinding> findings) {
        Map<String, List<RuleDefinition>> domains = rules.stream()
                .filter(rule -> DOMAIN_OPERATOR.equals(rule.condition().operator()))
                .collect(java.util.stream.Collectors.groupingBy(
                        rule -> rule.condition().parameters().get(VALUE_PARAMETER)));
        rules.stream()
                .filter(rule -> "sender-exact".equals(rule.condition().operator()))
                .filter(rule -> duplicatesDomain(rule, domains))
                .forEach(rule -> error(findings, rule, "sender-domain-duplicate",
                        "Exact sender duplicates a domain rule with identical actions"));
    }

    private static boolean duplicatesDomain(
            RuleDefinition sender, Map<String, List<RuleDefinition>> domains) {
        String address = sender.condition().parameters().get(VALUE_PARAMETER);
        String domain = address == null || !address.contains("@")
                ? "" : address.substring(address.indexOf('@') + 1);
        return domains.getOrDefault(domain, List.of()).stream()
                .anyMatch(candidate -> candidate.actions().equals(sender.actions()));
    }

    private static String signature(RuleDefinition rule) {
        return rule.condition() + "|" + rule.actions();
    }

    private static void error(
            List<RuleValidationFinding> findings,
            RuleDefinition rule,
            String code,
            String message) {
        findings.add(new RuleValidationFinding(
                rule.id().value(), RuleValidationSeverity.ERROR, code, message));
    }
}
