package dev.inboxpilot.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SenderLabelConflictDetector")
class SenderLabelConflictDetectorTest {

    private static final String SENDER = "sender@example.com";
    private static final String PERSONAL = "Personal";
    private static final String WORK = "Work";
    private static final String INBOX = "INBOX";
    private static final String SUBJECT = "Representative subject";
    private static final Instant DATE = Instant.parse("2026-08-01T10:00:00Z");

    @Test
    @DisplayName("reports incompatible labels with representative subjects")
    void reportsConflict() {
        InventoryStatistics statistics = new InventoryStatistics(
                1, 0, DATE, DATE,
                List.of(INBOX, PERSONAL, WORK), List.of(SUBJECT));
        Inventory inventory = new Inventory(
                List.of(new SenderInventory(new EmailAddress(SENDER), statistics)), List.of());

        List<SenderLabelConflict> conflicts = new SenderLabelConflictDetector()
                .detect(inventory, List.of(Set.of(PERSONAL, WORK)));

        assertThat(conflicts).singleElement().satisfies(conflict -> {
            assertThat(conflict.conflictingLabels()).containsExactly(PERSONAL, WORK);
            assertThat(conflict.representativeSubjects()).containsExactly(SUBJECT);
        });
    }
}
