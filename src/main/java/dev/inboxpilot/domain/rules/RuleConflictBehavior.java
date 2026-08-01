package dev.inboxpilot.domain.rules;

/** Behavior after this rule matches, explicitly represented in persisted YAML. */
public enum RuleConflictBehavior {
    CONTINUE,
    STOP
}
