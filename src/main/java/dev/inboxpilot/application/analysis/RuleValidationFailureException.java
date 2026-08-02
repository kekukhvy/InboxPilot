package dev.inboxpilot.application.analysis;

/** Stops analysis after persisting generated-rule validation errors. */
public class RuleValidationFailureException extends RuntimeException {

    public RuleValidationFailureException(String message) {
        super(message);
    }
}
