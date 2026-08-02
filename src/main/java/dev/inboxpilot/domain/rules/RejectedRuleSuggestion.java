package dev.inboxpilot.domain.rules;

/** Candidate deliberately omitted by the rule-generation safety policy. */
public record RejectedRuleSuggestion(
        String sender, String domain, int messageCount, String reason) {
}
