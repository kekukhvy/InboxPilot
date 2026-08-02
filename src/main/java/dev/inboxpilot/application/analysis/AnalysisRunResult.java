package dev.inboxpilot.application.analysis;

/** Summary of the read-only mailbox analysis. */
public record AnalysisRunResult(
        int processedMessages,
        int unclassifiedSenders,
        int unclassifiedDomains) {
}
