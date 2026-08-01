package dev.inboxpilot.domain.cleanup;

import dev.inboxpilot.domain.message.EmailAddress;
import dev.inboxpilot.domain.message.MessageId;
import java.time.Instant;
import java.util.Objects;

/** Minimal message metadata for stale-newsletter cleanup analysis. */
public record NewsletterMessage(
        MessageId id,
        EmailAddress sender,
        Instant receivedAt,
        boolean newsletter) {

    public NewsletterMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }
}
