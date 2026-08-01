package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.inventory.DomainInventory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Builds evidence-backed label creation, merge, and rename proposals. */
public final class LabelTaxonomyPlanner {

    private static final String DOMAIN_PREFIX = "Domain/";

    public List<LabelTaxonomyProposal> propose(
            UnclassifiedInventory unclassified,
            LabelStructureAnalysis labels,
            int minimumMessagesForNewLabel) {
        if (minimumMessagesForNewLabel < 1) {
            throw new IllegalArgumentException("Minimum message count must be positive");
        }
        List<LabelTaxonomyProposal> proposals = new ArrayList<>();
        unclassified.domains().stream()
                .filter(domain -> domain.statistics().messageCount() >= minimumMessagesForNewLabel)
                .map(LabelTaxonomyPlanner::createProposal)
                .forEach(proposals::add);
        labels.duplicateLabelGroups().stream()
                .map(LabelTaxonomyPlanner::mergeProposal)
                .forEach(proposals::add);
        labels.unusedOrEmptyLabels().stream()
                .filter(label -> !label.name().equals(label.name().strip()))
                .map(LabelTaxonomyPlanner::renameProposal)
                .forEach(proposals::add);
        return proposals.stream()
                .sorted(Comparator.comparing(LabelTaxonomyProposal::type)
                        .thenComparing(LabelTaxonomyProposal::proposedName))
                .toList();
    }

    private static LabelTaxonomyProposal createProposal(DomainInventory domain) {
        String name = DOMAIN_PREFIX + domain.domain().toLowerCase(Locale.ROOT);
        String evidence = domain.statistics().messageCount() + " unclassified messages from " + domain.domain();
        return new LabelTaxonomyProposal(LabelTaxonomyChangeType.CREATE, List.of(), name, evidence);
    }

    private static LabelTaxonomyProposal mergeProposal(List<LabelDefinition> duplicates) {
        LabelDefinition target = duplicates.get(0);
        List<String> sourceIds = duplicates.stream().map(LabelDefinition::id).toList();
        String evidence = duplicates.size() + " labels share the same normalized name";
        return new LabelTaxonomyProposal(
                LabelTaxonomyChangeType.MERGE, sourceIds, target.name().strip(), evidence);
    }

    private static LabelTaxonomyProposal renameProposal(LabelDefinition label) {
        return new LabelTaxonomyProposal(
                LabelTaxonomyChangeType.RENAME,
                List.of(label.id()),
                label.name().strip(),
                "Visible name contains surrounding whitespace");
    }
}
