package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.message.EmailAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Classifies newsletter and bulk-mail senders from headers and sender patterns. */
public final class BulkMailDetector {

    private static final String LIST_UNSUBSCRIBE = "list-unsubscribe";
    private static final String PRECEDENCE = "precedence";
    private static final List<String> BULK_PRECEDENCE = List.of("bulk", "list", "junk");
    private static final List<String> SENDER_PATTERNS =
            List.of("newsletter", "news", "notify", "notification", "no-reply", "noreply");

    public List<BulkMailSender> detect(Collection<MailSignal> messages) {
        Map<EmailAddress, Evidence> evidenceBySender = new LinkedHashMap<>();
        messages.forEach(message -> evidenceBySender
                .computeIfAbsent(message.sender(), ignored -> new Evidence())
                .accept(message));
        return evidenceBySender.entrySet().stream()
                .filter(entry -> entry.getValue().isBulk())
                .map(entry -> entry.getValue().result(entry.getKey()))
                .sorted(Comparator.comparingInt(BulkMailSender::messageCount).reversed()
                        .thenComparing(result -> result.sender().value()))
                .toList();
    }

    private static final class Evidence {
        private int messageCount;
        private final Set<String> signals = new LinkedHashSet<>();

        private void accept(MailSignal message) {
            messageCount++;
            normalizedHeaders(message.headers()).forEach(this::inspectHeader);
            if (matchesSenderPattern(message.sender())) {
                signals.add("sender-pattern");
            }
        }

        private void inspectHeader(String name, String value) {
            if (LIST_UNSUBSCRIBE.equals(name) && !value.isBlank()) {
                signals.add(LIST_UNSUBSCRIBE);
            }
            if (PRECEDENCE.equals(name) && BULK_PRECEDENCE.contains(value.strip().toLowerCase(Locale.ROOT))) {
                signals.add("precedence:" + value.strip().toLowerCase(Locale.ROOT));
            }
        }

        private boolean isBulk() {
            return !signals.isEmpty();
        }

        private BulkMailSender result(EmailAddress sender) {
            List<String> sortedSignals = new ArrayList<>(signals);
            sortedSignals.sort(Comparator.naturalOrder());
            return new BulkMailSender(sender, messageCount, sortedSignals);
        }
    }

    private static Map<String, String> normalizedHeaders(Map<String, String> headers) {
        Map<String, String> normalized = new LinkedHashMap<>();
        headers.forEach((name, value) -> normalized.put(name.toLowerCase(Locale.ROOT), value));
        return normalized;
    }

    private static boolean matchesSenderPattern(EmailAddress sender) {
        String localPart = sender.value().substring(0, sender.value().indexOf('@'));
        return SENDER_PATTERNS.stream().anyMatch(localPart::contains);
    }
}
