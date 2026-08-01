package dev.inboxpilot.domain.analysis;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.Objects;

/** Metadata-only input for cleanup candidate analysis. */
public record CleanupMessageProfile(
        MessageId id,
        EmailAddress sender,
        String subject,
        Instant receivedAt,
        boolean bulkMail) {

    public CleanupMessageProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }
}
