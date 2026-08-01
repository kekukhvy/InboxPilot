package dev.inboxpilot.application.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import dev.inboxpilot.application.port.InventoryReportStore;
import dev.inboxpilot.application.port.MessageSource;
import dev.inboxpilot.domain.inventory.Inventory;
import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import dev.inboxpilot.domain.message.MessageId;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");
    private static final Duration LOOKBACK = Duration.ofDays(30);

    @Test
    void scansConfiguredLookbackCapsMessagesAndWritesReports() {
        CapturingSource source = new CapturingSource(List.of(message("one"), message("two"), message("three")));
        CapturingReports reports = new CapturingReports();
        InventoryService service = new InventoryService(
                source, reports, LOOKBACK, 2, Clock.fixed(NOW, ZoneOffset.UTC));

        InventoryRunResult result = service.run();

        assertThat(source.since).isEqualTo(NOW.minus(LOOKBACK));
        assertThat(source.maximumMessages).isEqualTo(2);
        assertThat(result.processedMessages()).isEqualTo(2);
        assertThat(result.reports()).containsExactly(Path.of("reports/inventory.json"));
        assertThat(reports.inventory.senders()).hasSize(2);
    }

    private static MailMessage message(String id) {
        return new MailMessage(
                new MessageId(id), new EmailAddress(id + "@example.com"), id,
                NOW.minusSeconds(60), List.of());
    }

    private static final class CapturingSource implements MessageSource {
        private final List<MailMessage> messages;
        private Instant since;
        private int maximumMessages;

        private CapturingSource(List<MailMessage> messages) {
            this.messages = new ArrayList<>(messages);
        }

        @Override
        public List<MailMessage> fetchSince(Instant requestedSince) {
            since = requestedSince;
            return List.copyOf(messages);
        }

        @Override
        public List<MailMessage> fetchSince(Instant requestedSince, int requestedMaximum) {
            since = requestedSince;
            maximumMessages = requestedMaximum;
            return messages.stream().limit(requestedMaximum).toList();
        }
    }

    private static final class CapturingReports implements InventoryReportStore {
        private Inventory inventory;

        @Override
        public List<Path> write(Inventory requestedInventory) {
            inventory = requestedInventory;
            return List.of(Path.of("reports/inventory.json"));
        }
    }
}
