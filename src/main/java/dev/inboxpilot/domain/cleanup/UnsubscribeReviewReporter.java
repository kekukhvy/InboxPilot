package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.EmailAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Groups and deduplicates extracted unsubscribe capabilities by sender. */
public final class UnsubscribeReviewReporter {

    public UnsubscribeReviewReport create(Collection<UnsubscribeMessage> messages) {
        Map<EmailAddress, List<UnsubscribeMessage>> grouped = messages.stream()
                .collect(Collectors.groupingBy(UnsubscribeMessage::sender));
        List<UnsubscribeReviewEntry> entries = grouped.entrySet().stream()
                .map(entry -> reviewEntry(entry.getKey(), entry.getValue()))
                .filter(entry -> !entry.mailto().isEmpty() || !entry.https().isEmpty())
                .sorted(Comparator.comparing(entry -> entry.sender().value()))
                .toList();
        return new UnsubscribeReviewReport(entries);
    }

    private static UnsubscribeReviewEntry reviewEntry(
            EmailAddress sender, List<UnsubscribeMessage> messages) {
        List<URI> mailto = messages.stream()
                .flatMap(message -> message.capabilities().mailto().stream())
                .distinct().sorted(Comparator.comparing(URI::toString)).toList();
        List<URI> https = messages.stream()
                .flatMap(message -> message.capabilities().https().stream())
                .distinct().sorted(Comparator.comparing(URI::toString)).toList();
        return new UnsubscribeReviewEntry(
                sender,
                messages.stream().map(UnsubscribeMessage::messageId)
                        .distinct().sorted(Comparator.comparing(messageId -> messageId.value())).toList(),
                mailto, https);
    }
}
