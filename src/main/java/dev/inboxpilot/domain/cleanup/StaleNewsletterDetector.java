package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.EmailAddress;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Finds stale newsletter messages and groups them by exact sender. */
public final class StaleNewsletterDetector {

    public List<StaleNewsletterGroup> detect(
            Collection<NewsletterMessage> messages, Instant staleBefore) {
        Map<EmailAddress, List<NewsletterMessage>> bySender = messages.stream()
                .filter(NewsletterMessage::newsletter)
                .filter(message -> message.receivedAt().isBefore(staleBefore))
                .collect(Collectors.groupingBy(NewsletterMessage::sender));
        return bySender.entrySet().stream()
                .map(entry -> group(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(group -> group.sender().value()))
                .toList();
    }

    private static StaleNewsletterGroup group(
            EmailAddress sender, List<NewsletterMessage> messages) {
        List<NewsletterMessage> ordered = messages.stream()
                .sorted(Comparator.comparing(NewsletterMessage::receivedAt)
                        .thenComparing(message -> message.id().value()))
                .toList();
        return new StaleNewsletterGroup(
                sender,
                ordered.stream().map(NewsletterMessage::id).toList(),
                ordered.getFirst().receivedAt(),
                ordered.getLast().receivedAt());
    }
}
