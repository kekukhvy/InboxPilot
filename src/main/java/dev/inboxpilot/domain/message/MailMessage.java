package dev.inboxpilot.domain.message;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A mail message as the domain understands it: who sent it, when, what it is
 * about, and which labels it currently carries.
 *
 * <p>Deliberately free of any mail-provider type. A Gmail message, an IMAP
 * message, or a test fixture all become this record before any rule sees them,
 * which is what lets classification logic be tested without a mailbox
 * (ADR 0001).
 *
 * @param id          provider-independent identifier of the message
 * @param sender      the address the message was sent from
 * @param subject     the subject line; empty string when the message has none
 * @param receivedAt  when the message arrived
 * @param labels      labels currently applied; never {@code null}
 */
public record MailMessage(
        MessageId id,
        EmailAddress sender,
        String subject,
        Instant receivedAt,
        List<String> labels) {

    /**
     * @throws NullPointerException if any argument is {@code null}
     */
    public MailMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(labels, "labels");
        labels = List.copyOf(labels);
    }

    /**
     * Tells whether this message already carries the given label.
     *
     * @param label the label to look for
     * @return {@code true} if the label is present
     */
    public boolean hasLabel(String label) {
        return labels.contains(label);
    }
}
