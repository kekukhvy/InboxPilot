package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.net.URI;
import java.util.List;

/** Deduplicated manual unsubscribe mechanisms grouped by sender. */
public record UnsubscribeReviewEntry(
        EmailAddress sender,
        List<MessageId> messageIds,
        List<URI> mailto,
        List<URI> https) {

    public UnsubscribeReviewEntry {
        messageIds = List.copyOf(messageIds);
        mailto = List.copyOf(mailto);
        https = List.copyOf(https);
    }
}
