package dev.inboxpilot.domain.rules;

/** One actionable generated-rule validation finding. */
public record RuleValidationFinding(
        String ruleId,
        RuleValidationSeverity severity,
        String errorCode,
        String message) {
}
