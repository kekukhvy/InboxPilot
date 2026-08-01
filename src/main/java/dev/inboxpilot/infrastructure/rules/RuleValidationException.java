package dev.inboxpilot.infrastructure.rules;

/** Actionable failure raised when a persisted rule document is invalid. */
public final class RuleValidationException extends RuntimeException {

    public RuleValidationException(String message) {
        super(message);
    }

    public RuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
