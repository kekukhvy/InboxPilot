package dev.inboxpilot.domain.rules;

import java.util.List;

/** Deterministic results for every example embedded in one rule. */
public record RuleTestReport(RuleId ruleId, List<RuleTestResult> results) {

    public RuleTestReport {
        results = List.copyOf(results);
    }

    public boolean passed() {
        return results.stream().allMatch(RuleTestResult::passed);
    }
}
