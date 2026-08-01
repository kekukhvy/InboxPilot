package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.List;

/** Stale newsletter candidates grouped by exact sender. */
public record StaleNewsletterGroup(
        EmailAddress sender,
        List<MessageId> messageIds,
        Instant oldestReceivedAt,
        Instant newestReceivedAt) {

    public StaleNewsletterGroup {
        messageIds = List.copyOf(messageIds);
    }
}
