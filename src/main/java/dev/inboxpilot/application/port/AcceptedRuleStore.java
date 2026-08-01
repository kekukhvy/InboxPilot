package dev.inboxpilot.application.port;

import dev.inboxpilot.domain.rules.RuleSet;

/** Persists human-confirmed deterministic rules. */
public interface AcceptedRuleStore {

    void save(RuleSet ruleSet);
}
