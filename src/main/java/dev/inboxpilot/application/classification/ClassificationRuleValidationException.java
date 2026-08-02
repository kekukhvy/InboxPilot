package dev.inboxpilot.application.classification;

import dev.inboxpilot.domain.rules.RuleValidationFinding;
import java.util.List;

/** Stops classification before mailbox reads when the selected rules are unsafe. */
public final class ClassificationRuleValidationException extends RuntimeException {

    private final List<RuleValidationFinding> findings;

    public ClassificationRuleValidationException(List<RuleValidationFinding> findings) {
        super("Classification rules failed validation with " + findings.size() + " error(s)");
        this.findings = List.copyOf(findings);
    }

    public List<RuleValidationFinding> findings() {
        return findings;
    }
}
