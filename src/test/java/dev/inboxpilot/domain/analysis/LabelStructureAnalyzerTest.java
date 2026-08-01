package dev.inboxpilot.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LabelStructureAnalyzer")
class LabelStructureAnalyzerTest {

    private static final String FIRST_ID = "Label_1";
    private static final String SECOND_ID = "Label_2";
    private static final String EMPTY_ID = "Label_3";

    @Test
    @DisplayName("finds empty labels and duplicate names deterministically")
    void findsEmptyAndDuplicateLabels() {
        List<LabelDefinition> labels = List.of(
                new LabelDefinition(FIRST_ID, "Newsletters"),
                new LabelDefinition(SECOND_ID, " newsletters "),
                new LabelDefinition(EMPTY_ID, "Receipts"));
        Inventory inventory = new Inventory(List.of(senderWithLabel(FIRST_ID)), List.of());

        LabelStructureAnalysis analysis = new LabelStructureAnalyzer().analyze(labels, inventory);

        assertThat(analysis.unusedOrEmptyLabels())
                .extracting(LabelDefinition::id)
                .containsExactly(SECOND_ID, EMPTY_ID);
        assertThat(analysis.duplicateLabelGroups()).containsExactly(List.of(labels.get(0), labels.get(1)));
    }

    private static SenderInventory senderWithLabel(String label) {
        Instant receivedAt = Instant.parse("2026-01-01T00:00:00Z");
        InventoryStatistics statistics = new InventoryStatistics(
                1, 0, receivedAt, receivedAt, List.of(label), List.of("Example"));
        return new SenderInventory(new EmailAddress("sender@example.com"), statistics);
    }
}
