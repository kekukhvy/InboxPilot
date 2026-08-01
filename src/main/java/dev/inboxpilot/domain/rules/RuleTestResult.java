package dev.inboxpilot.domain.rules;

/** Actual outcome for one embedded rule example. */
public record RuleTestResult(String name, boolean expectedMatch, boolean actualMatch) {

    public boolean passed() {
        return expectedMatch == actualMatch;
    }
}
