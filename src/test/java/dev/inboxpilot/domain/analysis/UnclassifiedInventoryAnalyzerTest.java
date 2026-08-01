package dev.inboxpilot.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.inventory.DomainInventory;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.inventory.InventoryStatistics;
import dev.inboxpilot.domain.inventory.SenderInventory;
import dev.inboxpilot.domain.message.EmailAddress;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UnclassifiedInventoryAnalyzer")
class UnclassifiedInventoryAnalyzerTest {
    private static final String FIRST_SENDER = "busy@example.com";
    private static final String SECOND_SENDER = "quiet@example.com";
    private static final String CLASSIFIED_SENDER = "known@example.com";
    private static final String DOMAIN = "example.com";
    private static final String OTHER_DOMAIN = "other.test";
    private static final String USER_LABEL = "Label_1";
    private static final String SYSTEM_LABEL = "INBOX";
    private static final Instant DATE = Instant.parse("2026-08-01T10:00:00Z");
    private static final int HIGH_VOLUME = 10;
    private static final int LOW_VOLUME = 3;
    private static final int HIGH_UNREAD = 5;
    private static final int LOW_UNREAD = 1;

    @Test
    @DisplayName("excludes entries carrying a user label and ranks by volume then unread")
    void ranksUnclassifiedEntries() {
        Inventory inventory = new Inventory(
                List.of(
                        sender(SECOND_SENDER, LOW_VOLUME, HIGH_UNREAD, SYSTEM_LABEL),
                        sender(CLASSIFIED_SENDER, HIGH_VOLUME, HIGH_UNREAD, USER_LABEL),
                        sender(FIRST_SENDER, HIGH_VOLUME, LOW_UNREAD, SYSTEM_LABEL)),
                List.of(
                        domain(OTHER_DOMAIN, LOW_VOLUME, LOW_UNREAD, SYSTEM_LABEL),
                        domain(DOMAIN, HIGH_VOLUME, HIGH_UNREAD, SYSTEM_LABEL)));

        UnclassifiedInventory report = new UnclassifiedInventoryAnalyzer()
                .analyze(inventory, Set.of(USER_LABEL));

        assertThat(report.senders()).extracting(entry -> entry.sender().value())
                .containsExactly(FIRST_SENDER, SECOND_SENDER);
        assertThat(report.domains()).extracting(DomainInventory::domain)
                .containsExactly(DOMAIN, OTHER_DOMAIN);
    }

    private static SenderInventory sender(String value, int count, int unread, String label) {
        return new SenderInventory(new EmailAddress(value), statistics(count, unread, label));
    }

    private static DomainInventory domain(String value, int count, int unread, String label) {
        return new DomainInventory(value, statistics(count, unread, label));
    }

    private static InventoryStatistics statistics(int count, int unread, String label) {
        return new InventoryStatistics(count, unread, DATE, DATE, List.of(label), List.of());
    }
}
