package dev.inboxpilot.domain.inventory;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MailMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Aggregates mailbox messages by exact sender and sender domain. */
public final class InventoryAggregator {

    private static final String UNREAD_LABEL = "UNREAD";
    private static final int MAX_SAMPLE_SUBJECTS = 5;

    public Inventory aggregate(List<MailMessage> messages) {
        Map<EmailAddress, Accumulator> senders = new TreeMap<>(
                Comparator.comparing(EmailAddress::value));
        Map<String, Accumulator> domains = new TreeMap<>();
        messages.stream().sorted(Comparator.comparing(MailMessage::receivedAt)).forEach(message -> {
            senders.computeIfAbsent(message.sender(), ignored -> new Accumulator()).add(message);
            domains.computeIfAbsent(message.sender().domain(), ignored -> new Accumulator())
                    .add(message);
        });
        return new Inventory(toSenders(senders), toDomains(domains));
    }

    private static List<SenderInventory> toSenders(Map<EmailAddress, Accumulator> values) {
        return values.entrySet().stream()
                .map(entry -> new SenderInventory(entry.getKey(), entry.getValue().statistics()))
                .toList();
    }

    private static List<DomainInventory> toDomains(Map<String, Accumulator> values) {
        return values.entrySet().stream()
                .map(entry -> new DomainInventory(entry.getKey(), entry.getValue().statistics()))
                .toList();
    }

    private static final class Accumulator {
        private int messageCount;
        private int unreadCount;
        private Instant firstReceivedAt;
        private Instant lastReceivedAt;
        private final LinkedHashSet<String> labels = new LinkedHashSet<>();
        private final List<String> subjects = new ArrayList<>();

        void add(MailMessage message) {
            messageCount++;
            if (message.hasLabel(UNREAD_LABEL)) {
                unreadCount++;
            }
            updateDates(message.receivedAt());
            labels.addAll(message.labels());
            if (!message.subject().isBlank() && subjects.size() < MAX_SAMPLE_SUBJECTS) {
                subjects.add(message.subject());
            }
        }

        private void updateDates(Instant receivedAt) {
            if (firstReceivedAt == null || receivedAt.isBefore(firstReceivedAt)) {
                firstReceivedAt = receivedAt;
            }
            if (lastReceivedAt == null || receivedAt.isAfter(lastReceivedAt)) {
                lastReceivedAt = receivedAt;
            }
        }

        InventoryStatistics statistics() {
            List<String> sortedLabels = labels.stream().sorted().toList();
            return new InventoryStatistics(
                    messageCount, unreadCount, firstReceivedAt, lastReceivedAt,
                    sortedLabels, subjects);
        }
    }
}
