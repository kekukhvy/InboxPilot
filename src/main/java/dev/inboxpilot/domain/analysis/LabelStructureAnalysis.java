package dev.inboxpilot.domain.analysis;

import java.util.List;

/** Empty labels and groups whose visible names are duplicates. */
public record LabelStructureAnalysis(
        List<LabelDefinition> unusedOrEmptyLabels,
        List<List<LabelDefinition>> duplicateLabelGroups) {

    public LabelStructureAnalysis {
        unusedOrEmptyLabels = List.copyOf(unusedOrEmptyLabels);
        duplicateLabelGroups = duplicateLabelGroups.stream().map(List::copyOf).toList();
    }
}
