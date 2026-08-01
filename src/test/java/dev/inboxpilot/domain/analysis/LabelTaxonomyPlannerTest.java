package dev.inboxpilot.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LabelTaxonomyPlanner")
class LabelTaxonomyPlannerTest {

    @Test
    @DisplayName("proposes creates, merges, and canonical renames with evidence")
    void proposesTaxonomyChanges() {
        LabelDefinition canonical = new LabelDefinition("Label_1", "Newsletters");
        LabelDefinition duplicate = new LabelDefinition("Label_2", " newsletters ");
        LabelStructureAnalysis labels = new LabelStructureAnalysis(
                List.of(duplicate), List.of(List.of(canonical, duplicate)));
        UnclassifiedInventory unclassified = new UnclassifiedInventory(
                List.of(), List.of(domain("vendor.example", 50)));

        List<LabelTaxonomyProposal> proposals =
                new LabelTaxonomyPlanner().propose(unclassified, labels, 20);

        assertThat(proposals).extracting(LabelTaxonomyProposal::type)
                .containsExactly(
                        LabelTaxonomyChangeType.CREATE,
                        LabelTaxonomyChangeType.MERGE,
                        LabelTaxonomyChangeType.RENAME);
        assertThat(proposals).allMatch(proposal -> !proposal.evidence().isBlank());
    }

    private static DomainInventory domain(String domain, int messages) {
        Instant receivedAt = Instant.parse("2026-01-01T00:00:00Z");
        return new DomainInventory(domain, new InventoryStatistics(
                messages, 0, receivedAt, receivedAt, List.of("INBOX"), List.of()));
    }
}
