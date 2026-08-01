package dev.inboxpilot.domain.analysis;

import java.util.List;

/** One explainable label taxonomy proposal for later review. */
public record LabelTaxonomyProposal(
        LabelTaxonomyChangeType type,
        List<String> sourceLabelIds,
        String proposedName,
        String evidence) {

    public LabelTaxonomyProposal {
        sourceLabelIds = List.copyOf(sourceLabelIds);
    }
}
