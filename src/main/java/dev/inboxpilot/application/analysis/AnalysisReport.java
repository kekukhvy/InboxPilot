package dev.inboxpilot.application.analysis;

import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.rules.RuleSet;
import java.util.List;

/** Complete deterministic local-analysis model written as review artifacts. */
public record AnalysisReport(
        int processedMessages,
        List<SenderInventory> unlabeledSenders,
        List<AnalysisLabelConflict> labelConflicts,
        List<AnalysisCleanupCandidate> cleanupCandidates,
        RuleSet ruleSuggestions) {

    public AnalysisReport {
        unlabeledSenders = List.copyOf(unlabeledSenders);
        labelConflicts = List.copyOf(labelConflicts);
        cleanupCandidates = List.copyOf(cleanupCandidates);
    }
}
