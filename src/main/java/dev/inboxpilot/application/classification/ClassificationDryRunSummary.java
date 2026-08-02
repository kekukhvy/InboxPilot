package dev.inboxpilot.application.classification;

/** Counts for a complete read-only classification simulation. */
public record ClassificationDryRunSummary(
        int processedMessages,
        int matchedMessages,
        int unmatchedMessages,
        int alreadyCorrectlyLabeled,
        int messagesWithProposedChanges,
        int proposedLabelAdditions,
        int proposedLabelRemovals,
        int messagesMatchedByMultipleRules,
        int ruleConflicts) {
}
