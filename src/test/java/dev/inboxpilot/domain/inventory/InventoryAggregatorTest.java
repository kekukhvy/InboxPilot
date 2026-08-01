package dev.inboxpilot.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InventoryAggregator")
class InventoryAggregatorTest {

    private static final String FIRST_ID = "message-1";
    private static final String SECOND_ID = "message-2";
    private static final String THIRD_ID = "message-3";
    private static final String FOURTH_ID = "message-4";
    private static final String FIRST_SENDER = "news@example.com";
    private static final String SECOND_SENDER = "billing@example.com";
    private static final String OTHER_SENDER = "alerts@other.test";
    private static final String DOMAIN = "example.com";
    private static final String FIRST_SUBJECT = "First";
    private static final String SECOND_SUBJECT = "Second";
    private static final String THIRD_SUBJECT = "Third";
    private static final String OTHER_SUBJECT = "Other";
    private static final String INBOX = "INBOX";
    private static final String UNREAD = "UNREAD";
    private static final String IMPORTANT = "IMPORTANT";
    private static final Instant FIRST_DATE = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant SECOND_DATE = Instant.parse("2026-08-02T10:00:00Z");
    private static final Instant THIRD_DATE = Instant.parse("2026-08-03T10:00:00Z");
    private static final int TWO_MESSAGES = 2;
    private static final int THREE_MESSAGES = 3;

    @Test
    @DisplayName("aggregates exact sender statistics")
    void aggregatesSenderStatistics() {
        Inventory inventory = aggregator().aggregate(messages());

        SenderInventory sender = inventory.senders().stream()
                .filter(entry -> entry.sender().value().equals(FIRST_SENDER))
                .findFirst()
                .orElseThrow();

        assertThat(sender.statistics().messageCount()).isEqualTo(TWO_MESSAGES);
        assertThat(sender.statistics().unreadCount()).isEqualTo(1);
        assertThat(sender.statistics().firstReceivedAt()).isEqualTo(FIRST_DATE);
        assertThat(sender.statistics().lastReceivedAt()).isEqualTo(SECOND_DATE);
        assertThat(sender.statistics().currentLabels()).containsExactly(IMPORTANT, INBOX, UNREAD);
        assertThat(sender.statistics().sampleSubjects())
                .containsExactly(FIRST_SUBJECT, SECOND_SUBJECT);
    }

    @Test
    @DisplayName("combines all senders belonging to a domain")
    void aggregatesDomainStatistics() {
        Inventory inventory = aggregator().aggregate(messages());

        DomainInventory domain = inventory.domains().stream()
                .filter(entry -> entry.domain().equals(DOMAIN))
                .findFirst()
                .orElseThrow();

        assertThat(domain.statistics().messageCount()).isEqualTo(THREE_MESSAGES);
        assertThat(domain.statistics().unreadCount()).isEqualTo(TWO_MESSAGES);
        assertThat(domain.statistics().firstReceivedAt()).isEqualTo(FIRST_DATE);
        assertThat(domain.statistics().lastReceivedAt()).isEqualTo(THIRD_DATE);
    }

    private static InventoryAggregator aggregator() {
        return new InventoryAggregator();
    }

    private static List<MailMessage> messages() {
        return List.of(
                message(FIRST_ID, FIRST_SENDER, FIRST_SUBJECT, FIRST_DATE, INBOX, UNREAD),
                message(SECOND_ID, FIRST_SENDER, SECOND_SUBJECT, SECOND_DATE, IMPORTANT),
                message(THIRD_ID, SECOND_SENDER, THIRD_SUBJECT, THIRD_DATE, UNREAD),
                message(FOURTH_ID, OTHER_SENDER, OTHER_SUBJECT, THIRD_DATE, INBOX));
    }

    private static MailMessage message(
            String id,
            String sender,
            String subject,
            Instant receivedAt,
            String... labels) {
        return new MailMessage(
                new MessageId(id), new EmailAddress(sender), subject,
                receivedAt, List.of(labels));
    }
}
