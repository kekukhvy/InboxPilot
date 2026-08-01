package dev.inboxpilot.application.model;

import dev.inboxpilot.domain.message.MailMessage;
import java.util.List;
import java.util.Objects;
import java.time.Instant;

/** Resumable scan progress together with the messages aggregated so far. */
public record ScanCheckpoint(String fingerprint, Instant since, List<MailMessage> messages) {

    public ScanCheckpoint {
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(since, "since");
        Objects.requireNonNull(messages, "messages");
        messages = List.copyOf(messages);
    }
}
